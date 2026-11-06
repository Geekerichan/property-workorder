package com.prop.wo.modules.basedata.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.prop.wo.common.exception.BizException;
import com.prop.wo.modules.basedata.entity.Building;
import com.prop.wo.modules.basedata.entity.Community;
import com.prop.wo.modules.basedata.entity.House;
import com.prop.wo.modules.basedata.entity.Unit;
import com.prop.wo.modules.basedata.mapper.BuildingMapper;
import com.prop.wo.modules.basedata.mapper.CommunityMapper;
import com.prop.wo.modules.basedata.mapper.HouseMapper;
import com.prop.wo.modules.basedata.mapper.UnitMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 五级楼盘档案：小区 → 楼栋 → 单元 → 房屋 → 业主，删除前做关联校验
 */
@Service
@RequiredArgsConstructor
public class StructureService {

    private final CommunityMapper communityMapper;
    private final BuildingMapper buildingMapper;
    private final UnitMapper unitMapper;
    private final HouseMapper houseMapper;

    /** 小区-楼栋-单元 三级树 */
    public List<Map<String, Object>> tree() {
        List<Community> communities = communityMapper.selectList(
                new LambdaQueryWrapper<Community>().orderByAsc(Community::getId));
        List<Building> buildings = buildingMapper.selectList(
                new LambdaQueryWrapper<Building>().orderByAsc(Building::getId));
        List<Unit> units = unitMapper.selectList(new LambdaQueryWrapper<Unit>().orderByAsc(Unit::getId));
        List<House> houses = houseMapper.selectList(new LambdaQueryWrapper<House>());

        Map<Long, Long> unitHouseCount = houses.stream()
                .filter(h -> h.getUnitId() != null)
                .collect(Collectors.groupingBy(House::getUnitId, Collectors.counting()));
        Map<Long, List<Unit>> unitsByBuilding = units.stream()
                .collect(Collectors.groupingBy(Unit::getBuildingId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, List<Building>> buildingsByCommunity = buildings.stream()
                .collect(Collectors.groupingBy(Building::getCommunityId, LinkedHashMap::new, Collectors.toList()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Community community : communities) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", community.getId());
            node.put("name", community.getName());
            node.put("address", community.getAddress());
            List<Map<String, Object>> buildingNodes = new ArrayList<>();
            for (Building building : buildingsByCommunity.getOrDefault(community.getId(), List.of())) {
                Map<String, Object> bNode = new LinkedHashMap<>();
                bNode.put("id", building.getId());
                bNode.put("name", building.getName());
                bNode.put("floors", building.getFloors());
                List<Map<String, Object>> unitNodes = new ArrayList<>();
                for (Unit unit : unitsByBuilding.getOrDefault(building.getId(), List.of())) {
                    Map<String, Object> uNode = new LinkedHashMap<>();
                    uNode.put("id", unit.getId());
                    uNode.put("name", unit.getName());
                    uNode.put("houseCount", unitHouseCount.getOrDefault(unit.getId(), 0L));
                    unitNodes.add(uNode);
                }
                bNode.put("units", unitNodes);
                buildingNodes.add(bNode);
            }
            node.put("buildings", buildingNodes);
            result.add(node);
        }
        return result;
    }

    public Community saveCommunity(Community community) {
        if (!StringUtils.hasText(community.getName())) {
            throw new BizException("请输入小区名称");
        }
        if (community.getId() == null) {
            communityMapper.insert(community);
        } else {
            communityMapper.updateById(community);
        }
        return community;
    }

    public Building saveBuilding(Building building) {
        if (building.getCommunityId() == null || !StringUtils.hasText(building.getName())) {
            throw new BizException("请选择小区并填写楼栋名称");
        }
        if (communityMapper.selectById(building.getCommunityId()) == null) {
            throw new BizException("小区不存在");
        }
        if (building.getId() == null) {
            buildingMapper.insert(building);
        } else {
            buildingMapper.updateById(building);
        }
        return building;
    }

    public Unit saveUnit(Unit unit) {
        if (unit.getBuildingId() == null || !StringUtils.hasText(unit.getName())) {
            throw new BizException("请选择楼栋并填写单元名称");
        }
        Building building = buildingMapper.selectById(unit.getBuildingId());
        if (building == null) {
            throw new BizException("楼栋不存在");
        }
        unit.setCommunityId(building.getCommunityId());
        if (unit.getId() == null) {
            unitMapper.insert(unit);
        } else {
            unitMapper.updateById(unit);
        }
        return unit;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteCommunity(Long id) {
        Long buildings = buildingMapper.selectCount(new LambdaQueryWrapper<Building>().eq(Building::getCommunityId, id));
        if (buildings != null && buildings > 0) {
            throw new BizException("该小区下存在楼栋，禁止删除");
        }
        communityMapper.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteBuilding(Long id) {
        Long units = unitMapper.selectCount(new LambdaQueryWrapper<Unit>().eq(Unit::getBuildingId, id));
        Long houses = houseMapper.selectCount(new LambdaQueryWrapper<House>().eq(House::getBuildingId, id));
        if ((units != null && units > 0) || (houses != null && houses > 0)) {
            throw new BizException("该楼栋下存在单元或房屋，禁止删除");
        }
        buildingMapper.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteUnit(Long id) {
        Long houses = houseMapper.selectCount(new LambdaQueryWrapper<House>().eq(House::getUnitId, id));
        if (houses != null && houses > 0) {
            throw new BizException("该单元下存在房屋，禁止删除");
        }
        unitMapper.deleteById(id);
    }
}
