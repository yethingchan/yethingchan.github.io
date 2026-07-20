package com.example.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.admin.common.utils.StringUtils;
import com.example.admin.domain.SysConfig;
import com.example.admin.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 系统参数配置（数据库连接外的业务参数：主题、导出路径、日志天数…）
 */
@Service
@RequiredArgsConstructor
public class ConfigService {

    private final SysConfigMapper configMapper;

    public IPage<SysConfig> list(IPage<SysConfig> page, String configName, String configKey, String configType) {
        LambdaQueryWrapper<SysConfig> q = new LambdaQueryWrapper<>();
        q.like(StringUtils.isNotBlank(configName), SysConfig::getConfigName, configName);
        q.like(StringUtils.isNotBlank(configKey), SysConfig::getConfigKey, configKey);
        q.eq(StringUtils.isNotBlank(configType), SysConfig::getConfigType, configType);
        q.orderByDesc(SysConfig::getConfigId);
        return configMapper.selectPage(page, q);
    }

    public SysConfig getByKey(String configKey) {
        return configMapper.selectOne(
                new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, configKey));
    }

    public SysConfig getById(Long id) {
        return configMapper.selectById(id);
    }

    public void add(SysConfig c) {
        configMapper.insert(c);
    }

    public void update(SysConfig c) {
        configMapper.updateById(c);
    }

    public void delete(List<Long> ids) {
        configMapper.deleteBatchIds(ids);
    }
}
