package com.prop.wo.modules.basedata.service;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.prop.wo.common.entity.PageQuery;
import com.prop.wo.common.exception.BizException;
import com.prop.wo.common.result.PageResult;
import com.prop.wo.modules.basedata.dto.HouseExcelRow;
import com.prop.wo.modules.basedata.entity.Building;
import com.prop.wo.modules.basedata.entity.Community;
import com.prop.wo.modules.basedata.entity.House;
import com.prop.wo.modules.basedata.entity.HouseOwner;
import com.prop.wo.modules.basedata.entity.OwnerUser;
import com.prop.wo.modules.basedata.entity.Unit;
import com.prop.wo.modules.basedata.mapper.BuildingMapper;
import com.prop.wo.modules.basedata.mapper.CommunityMapper;
import com.prop.wo.modules.basedata.mapper.HouseMapper;
import com.prop.wo.modules.basedata.mapper.HouseOwnerMapper;
import com.prop.wo.modules.basedata.mapper.OwnerUserMapper;
import com.prop.wo.modules.basedata.mapper.UnitMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 房屋与业主档案：一房多业主、房号带出业主、Excel 批量导入导出
 */
@Service
@RequiredArgsConstructor
public class HouseService {

    private final HouseMapper houseMapper;
    private final HouseOwnerMapper houseOwnerMapper;
    private final OwnerUserMapper ownerUserMapper;
    private final CommunityMapper communityMapper;
    private final BuildingMapper buildingMapper;
    private final UnitMapper unitMapper;

    public PageResult<Map<String, Object>> page(Long communityId, Long buildingId, Long unitId,
                                               String roomNo, String ownerKeyword, PageQuery query) {
        LambdaQueryWrapper<House> wrapper = new LambdaQueryWrapper<House>()
                .eq(communityId != null, House::getCommunityId, communityId)
                .eq(buildingId != null, House::getBuildingId, buildingId)
                .eq(unitId != null, House::getUnitId, unitId)
                .like(StringUtils.hasText(roomNo), House::getRoomNo, roomNo)
                .orderByAsc(House::getUnitId).orderByAsc(House::getRoomNo);
        // 业主关键字过滤：先按业主档案定位房屋，避免分页后在内存里过滤导致总数不准
        if (StringUtils.hasText(ownerKeyword)) {
            List<Long> ownerIds = ownerUserMapper.selectList(new LambdaQueryWrapper<OwnerUser>()
                            .like(OwnerUser::getName, ownerKeyword).or().like(OwnerUser::getPhone, ownerKeyword))
                    .stream().map(OwnerUser::getId).toList();
            if (ownerIds.isEmpty()) {
                return PageResult.of(0, List.of());
            }
            List<Long> houseIds = houseOwnerMapper.selectList(new LambdaQueryWrapper<HouseOwner>()
                            .in(HouseOwner::getOwnerId, ownerIds))
                    .stream().map(HouseOwner::getHouseId).distinct().toList();
            if (houseIds.isEmpty()) {
                return PageResult.of(0, List.of());
            }
            wrapper.in(House::getId, houseIds);
        }
        Page<House> page = houseMapper.selectPage(new Page<>(query.safePageNum(), query.safePageSize()), wrapper);

        Map<Long, String> communityNames = nameMap(communityMapper.selectList(null), Community::getId, Community::getName);
        Map<Long, String> buildingNames = nameMap(buildingMapper.selectList(null), Building::getId, Building::getName);
        Map<Long, String> unitNames = nameMap(unitMapper.selectList(null), Unit::getId, Unit::getName);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (House house : page.getRecords()) {
            List<Map<String, Object>> owners = ownersOf(house.getId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", house.getId());
            row.put("communityId", house.getCommunityId());
            row.put("communityName", communityNames.get(house.getCommunityId()));
            row.put("buildingName", buildingNames.get(house.getBuildingId()));
            row.put("unitName", unitNames.get(house.getUnitId()));
            row.put("roomNo", house.getRoomNo());
            row.put("floorNo", house.getFloorNo());
            row.put("area", house.getArea());
            row.put("houseType", house.getHouseType());
            row.put("status", house.getStatus());
            row.put("remark", house.getRemark());
            row.put("owners", owners);
            rows.add(row);
        }
        return PageResult.of(page.getTotal(), rows);
    }

    private <T> Map<Long, String> nameMap(List<T> list, java.util.function.Function<T, Long> idFn,
                                          java.util.function.Function<T, String> nameFn) {
        Map<Long, String> map = new HashMap<>();
        list.forEach(item -> map.put(idFn.apply(item), nameFn.apply(item)));
        return map;
    }

    public List<Map<String, Object>> ownersOf(Long houseId) {
        List<HouseOwner> relations = houseOwnerMapper.selectList(
                new LambdaQueryWrapper<HouseOwner>().eq(HouseOwner::getHouseId, houseId));
        List<Map<String, Object>> owners = new ArrayList<>();
        for (HouseOwner relation : relations) {
            OwnerUser owner = ownerUserMapper.selectById(relation.getOwnerId());
            if (owner == null) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("relationId", relation.getId());
            row.put("ownerId", owner.getId());
            row.put("ownerName", owner.getName());
            row.put("phone", owner.getPhone());
            row.put("relation", relation.getRelation());
            row.put("isPrimary", relation.getIsPrimary());
            owners.add(row);
        }
        return owners;
    }

    @Transactional(rollbackFor = Exception.class)
    public House save(House house) {
        if (house.getUnitId() == null || !StringUtils.hasText(house.getRoomNo())) {
            throw new BizException("请选择单元并填写房号");
        }
        Unit unit = unitMapper.selectById(house.getUnitId());
        if (unit == null) {
            throw new BizException("单元不存在");
        }
        house.setCommunityId(unit.getCommunityId());
        house.setBuildingId(unit.getBuildingId());
        if (house.getStatus() == null) {
            house.setStatus("NORMAL");
        }
        if (house.getId() == null) {
            houseMapper.insert(house);
        } else {
            houseMapper.updateById(house);
        }
        return house;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long owners = houseOwnerMapper.selectCount(new LambdaQueryWrapper<HouseOwner>().eq(HouseOwner::getHouseId, id));
        if (owners != null && owners > 0) {
            throw new BizException("该房屋已绑定业主，请先解绑后再删除");
        }
        houseMapper.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void bindOwner(Long houseId, Long ownerId, String relation, Boolean primary) {
        House house = houseMapper.selectById(houseId);
        if (house == null) {
            throw new BizException("房屋不存在");
        }
        OwnerUser owner = ownerUserMapper.selectById(ownerId);
        if (owner == null) {
            throw new BizException("业主不存在");
        }
        Long exists = houseOwnerMapper.selectCount(new LambdaQueryWrapper<HouseOwner>()
                .eq(HouseOwner::getHouseId, houseId).eq(HouseOwner::getOwnerId, ownerId));
        if (exists != null && exists > 0) {
            throw new BizException("该业主已绑定此房屋");
        }
        HouseOwner link = new HouseOwner();
        link.setHouseId(houseId);
        link.setOwnerId(ownerId);
        link.setRelation(relation == null ? "OWNER" : relation);
        link.setIsPrimary(Boolean.TRUE.equals(primary) ? 1 : 0);
        houseOwnerMapper.insert(link);
    }

    public void unbindOwner(Long relationId) {
        houseOwnerMapper.deleteById(relationId);
    }

    @Transactional(rollbackFor = Exception.class)
    public OwnerUser saveOwner(OwnerUser owner) {
        if (!StringUtils.hasText(owner.getName())) {
            throw new BizException("请输入业主姓名");
        }
        if (owner.getPhone() == null || !owner.getPhone().matches("^1\\d{10}$")) {
            throw new BizException("业主手机号需为 11 位");
        }
        OwnerUser exists = ownerUserMapper.selectOne(new LambdaQueryWrapper<OwnerUser>()
                .eq(OwnerUser::getPhone, owner.getPhone()).last("limit 1"));
        if (exists != null && !Objects.equals(exists.getId(), owner.getId())) {
            throw new BizException("该手机号已存在业主档案");
        }
        if (owner.getId() == null) {
            ownerUserMapper.insert(owner);
        } else {
            ownerUserMapper.updateById(owner);
        }
        return owner;
    }

    /** 建单时按房号带出房屋与主业主信息 */
    public Map<String, Object> lookup(String roomNo) {
        if (!StringUtils.hasText(roomNo)) {
            throw new BizException("请输入房号");
        }
        List<House> houses = houseMapper.selectList(new LambdaQueryWrapper<House>()
                .eq(House::getRoomNo, roomNo).last("limit 5"));
        if (houses.isEmpty()) {
            throw new BizException("未找到该房号，请核对或先在档案中补录");
        }
        House house = houses.get(0);
        List<Map<String, Object>> owners = ownersOf(house.getId());
        Map<String, Object> primary = owners.stream()
                .filter(o -> Integer.valueOf(1).equals(o.get("isPrimary"))).findFirst()
                .orElse(owners.isEmpty() ? Map.of() : owners.get(0));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("houseId", house.getId());
        result.put("roomNo", house.getRoomNo());
        result.put("floorNo", house.getFloorNo());
        result.put("communityId", house.getCommunityId());
        result.put("buildingId", house.getBuildingId());
        result.put("unitId", house.getUnitId());
        result.put("ownerName", primary.get("ownerName"));
        result.put("ownerPhone", primary.get("phone"));
        result.put("owners", owners);
        return result;
    }

    /** 业主端：按手机号查询绑定房屋 */
    public List<Map<String, Object>> housesOfOwner(Long ownerId) {
        List<HouseOwner> relations = houseOwnerMapper.selectList(
                new LambdaQueryWrapper<HouseOwner>().eq(HouseOwner::getOwnerId, ownerId));
        List<Map<String, Object>> list = new ArrayList<>();
        for (HouseOwner relation : relations) {
            House house = houseMapper.selectById(relation.getHouseId());
            if (house == null) {
                continue;
            }
            Unit unit = unitMapper.selectById(house.getUnitId());
            Building building = buildingMapper.selectById(house.getBuildingId());
            Community community = communityMapper.selectById(house.getCommunityId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("houseId", house.getId());
            row.put("fullName", (community == null ? "" : community.getName())
                    + (building == null ? "" : building.getName())
                    + (unit == null ? "" : unit.getName()) + house.getRoomNo());
            row.put("roomNo", house.getRoomNo());
            row.put("communityId", house.getCommunityId());
            list.add(row);
        }
        return list;
    }

    /** 业主端登录校验：手机号与房号匹配 */
    public boolean matchesOwner(String phone, String roomNo) {
        OwnerUser owner = ownerUserMapper.selectOne(new LambdaQueryWrapper<OwnerUser>()
                .eq(OwnerUser::getPhone, phone).last("limit 1"));
        if (owner == null) {
            return false;
        }
        List<HouseOwner> relations = houseOwnerMapper.selectList(
                new LambdaQueryWrapper<HouseOwner>().eq(HouseOwner::getOwnerId, owner.getId()));
        for (HouseOwner relation : relations) {
            House house = houseMapper.selectById(relation.getHouseId());
            if (house != null && house.getRoomNo().equals(roomNo)) {
                return true;
            }
        }
        return false;
    }

    public OwnerUser ownerByPhone(String phone) {
        return ownerUserMapper.selectOne(new LambdaQueryWrapper<OwnerUser>()
                .eq(OwnerUser::getPhone, phone).last("limit 1"));
    }

    /** Excel 导入：返回错误行提示，成功导入的行直接入库 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importExcel(MultipartFile file) {
        List<HouseExcelRow> rows;
        try {
            rows = EasyExcel.read(file.getInputStream()).head(HouseExcelRow.class).sheet().doReadSync();
        } catch (IOException e) {
            throw new BizException("Excel 读取失败，请使用下载的模板");
        }
        List<String> errors = new ArrayList<>();
        int success = 0;
        for (int i = 0; i < rows.size(); i++) {
            HouseExcelRow row = rows.get(i);
            int line = i + 2;
            try {
                if (!StringUtils.hasText(row.getCommunityName()) || !StringUtils.hasText(row.getRoomNo())) {
                    errors.add("第 " + line + " 行：小区与房号不能为空");
                    continue;
                }
                Community community = communityMapper.selectOne(new LambdaQueryWrapper<Community>()
                        .eq(Community::getName, row.getCommunityName()).last("limit 1"));
                if (community == null) {
                    errors.add("第 " + line + " 行：小区【" + row.getCommunityName() + "】不存在");
                    continue;
                }
                Building building = buildingMapper.selectOne(new LambdaQueryWrapper<Building>()
                        .eq(Building::getCommunityId, community.getId())
                        .eq(Building::getName, row.getBuildingName()).last("limit 1"));
                if (building == null) {
                    errors.add("第 " + line + " 行：楼栋【" + row.getBuildingName() + "】不存在");
                    continue;
                }
                Unit unit = unitMapper.selectOne(new LambdaQueryWrapper<Unit>()
                        .eq(Unit::getBuildingId, building.getId())
                        .eq(Unit::getName, row.getUnitName()).last("limit 1"));
                if (unit == null) {
                    errors.add("第 " + line + " 行：单元【" + row.getUnitName() + "】不存在");
                    continue;
                }
                if (StringUtils.hasText(row.getOwnerPhone()) && !row.getOwnerPhone().matches("^1\\d{10}$")) {
                    errors.add("第 " + line + " 行：业主手机号需为 11 位");
                    continue;
                }
                House house = houseMapper.selectOne(new LambdaQueryWrapper<House>()
                        .eq(House::getUnitId, unit.getId()).eq(House::getRoomNo, row.getRoomNo()).last("limit 1"));
                if (house == null) {
                    house = new House();
                    house.setCommunityId(community.getId());
                    house.setBuildingId(building.getId());
                    house.setUnitId(unit.getId());
                    house.setRoomNo(row.getRoomNo());
                    house.setFloorNo(row.getFloorNo());
                    house.setArea(row.getArea());
                    house.setHouseType(StringUtils.hasText(row.getHouseType()) ? row.getHouseType() : "住宅");
                    house.setStatus("NORMAL");
                    house.setRemark(row.getRemark());
                    houseMapper.insert(house);
                }
                if (StringUtils.hasText(row.getOwnerPhone()) && StringUtils.hasText(row.getOwnerName())) {
                    OwnerUser owner = ownerByPhone(row.getOwnerPhone());
                    if (owner == null) {
                        owner = new OwnerUser();
                        owner.setPhone(row.getOwnerPhone());
                        owner.setName(row.getOwnerName());
                        ownerUserMapper.insert(owner);
                    }
                    Long exists = houseOwnerMapper.selectCount(new LambdaQueryWrapper<HouseOwner>()
                            .eq(HouseOwner::getHouseId, house.getId()).eq(HouseOwner::getOwnerId, owner.getId()));
                    if (exists == null || exists == 0) {
                        HouseOwner link = new HouseOwner();
                        link.setHouseId(house.getId());
                        link.setOwnerId(owner.getId());
                        link.setRelation(StringUtils.hasText(row.getRelation()) ? row.getRelation() : "OWNER");
                        link.setIsPrimary(1);
                        houseOwnerMapper.insert(link);
                    }
                }
                success++;
            } catch (Exception e) {
                errors.add("第 " + line + " 行：导入失败（" + e.getMessage() + "）");
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", rows.size());
        result.put("success", success);
        result.put("errors", errors);
        return result;
    }

    /** 导出房屋档案（含业主与工单必填信息） */
    public List<HouseExcelRow> exportRows() {
        List<House> houses = houseMapper.selectList(new LambdaQueryWrapper<House>()
                .orderByAsc(House::getUnitId).orderByAsc(House::getRoomNo));
        Map<Long, String> communityNames = nameMap(communityMapper.selectList(null), Community::getId, Community::getName);
        Map<Long, String> buildingNames = nameMap(buildingMapper.selectList(null), Building::getId, Building::getName);
        Map<Long, String> unitNames = nameMap(unitMapper.selectList(null), Unit::getId, Unit::getName);
        List<HouseExcelRow> rows = new ArrayList<>();
        for (House house : houses) {
            HouseExcelRow row = new HouseExcelRow();
            row.setCommunityName(communityNames.get(house.getCommunityId()));
            row.setBuildingName(buildingNames.get(house.getBuildingId()));
            row.setUnitName(unitNames.get(house.getUnitId()));
            row.setRoomNo(house.getRoomNo());
            row.setFloorNo(house.getFloorNo());
            row.setArea(house.getArea());
            row.setHouseType(house.getHouseType());
            row.setRemark(house.getRemark());
            List<Map<String, Object>> owners = ownersOf(house.getId());
            if (!owners.isEmpty()) {
                row.setOwnerName(String.valueOf(owners.get(0).get("ownerName")));
                row.setOwnerPhone(String.valueOf(owners.get(0).get("phone")));
                row.setRelation(String.valueOf(owners.get(0).get("relation")));
            }
            rows.add(row);
        }
        return rows;
    }
}
