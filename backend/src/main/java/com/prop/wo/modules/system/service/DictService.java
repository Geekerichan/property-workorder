package com.prop.wo.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.prop.wo.common.exception.BizException;
import com.prop.wo.modules.system.entity.SysConfig;
import com.prop.wo.modules.system.entity.SysDict;
import com.prop.wo.modules.system.entity.SysDictItem;
import com.prop.wo.modules.system.mapper.SysConfigMapper;
import com.prop.wo.modules.system.mapper.SysDictItemMapper;
import com.prop.wo.modules.system.mapper.SysDictMapper;
import com.prop.wo.modules.system.dto.SystemDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 字典与参数配置
 */
@Service
@RequiredArgsConstructor
public class DictService {

    private final SysDictMapper dictMapper;
    private final SysDictItemMapper itemMapper;
    private final SysConfigMapper configMapper;

    public List<SysDict> listDict() {
        return dictMapper.selectList(new LambdaQueryWrapper<SysDict>().orderByAsc(SysDict::getId));
    }

    public List<SysDictItem> items(String dictType) {
        return itemMapper.selectList(new LambdaQueryWrapper<SysDictItem>()
                .eq(SysDictItem::getDictType, dictType)
                .eq(SysDictItem::getStatus, "ENABLED")
                .orderByAsc(SysDictItem::getSortNo));
    }

    public List<SysDictItem> allItems() {
        return itemMapper.selectList(new LambdaQueryWrapper<SysDictItem>()
                .eq(SysDictItem::getStatus, "ENABLED")
                .orderByAsc(SysDictItem::getDictType).orderByAsc(SysDictItem::getSortNo));
    }

    public SysDict saveDict(SystemDtos.DictSaveReq req) {
        SysDict dict = req.id() == null ? new SysDict() : dictMapper.selectById(req.id());
        if (dict == null) {
            throw BizException.of("字典不存在");
        }
        Long same = dictMapper.selectCount(new LambdaQueryWrapper<SysDict>()
                .eq(SysDict::getDictType, req.dictType())
                .ne(req.id() != null, SysDict::getId, req.id()));
        if (same != null && same > 0) {
            throw BizException.of("字典类型已存在");
        }
        dict.setDictType(req.dictType());
        dict.setDictName(req.dictName());
        dict.setStatus(req.status() == null ? "ENABLED" : req.status());
        dict.setRemark(req.remark());
        if (dict.getId() == null) {
            dictMapper.insert(dict);
        } else {
            dictMapper.updateById(dict);
        }
        return dict;
    }

    public SysDictItem saveItem(SystemDtos.DictItemSaveReq req) {
        SysDictItem item = req.id() == null ? new SysDictItem() : itemMapper.selectById(req.id());
        if (item == null) {
            throw BizException.of("字典项不存在");
        }
        item.setDictType(req.dictType());
        item.setItemLabel(req.itemLabel());
        item.setItemValue(req.itemValue());
        item.setSortNo(req.sortNo() == null ? 0 : req.sortNo());
        item.setStatus(req.status() == null ? "ENABLED" : req.status());
        item.setRemark(req.remark());
        if (item.getId() == null) {
            itemMapper.insert(item);
        } else {
            itemMapper.updateById(item);
        }
        return item;
    }

    public void deleteItem(Long id) {
        itemMapper.deleteById(id);
    }

    public List<SysConfig> listConfig() {
        return configMapper.selectList(new LambdaQueryWrapper<SysConfig>().orderByAsc(SysConfig::getId));
    }

    public String configValue(String key, String defaultValue) {
        SysConfig config = configMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, key).last("limit 1"));
        return config == null ? defaultValue : config.getConfigValue();
    }

    public int configInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(configValue(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public SysConfig saveConfig(SystemDtos.ConfigSaveReq req) {
        SysConfig config = configMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, req.configKey()).last("limit 1"));
        if (config == null) {
            config = new SysConfig();
            config.setConfigKey(req.configKey());
        }
        config.setConfigValue(req.configValue());
        config.setConfigName(req.configName());
        config.setRemark(req.remark());
        if (config.getId() == null) {
            configMapper.insert(config);
        } else {
            configMapper.updateById(config);
        }
        return config;
    }
}
