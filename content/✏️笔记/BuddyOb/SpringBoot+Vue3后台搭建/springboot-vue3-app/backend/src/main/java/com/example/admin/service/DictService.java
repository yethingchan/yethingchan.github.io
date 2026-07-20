package com.example.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.admin.common.utils.StringUtils;
import com.example.admin.domain.SysDictData;
import com.example.admin.domain.SysDictType;
import com.example.admin.mapper.SysDictDataMapper;
import com.example.admin.mapper.SysDictTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 字典管理：字典类型 + 字典数据（下拉框统一读取，避免硬编码）
 */
@Service
@RequiredArgsConstructor
public class DictService {

    private final SysDictTypeMapper dictTypeMapper;
    private final SysDictDataMapper dictDataMapper;

    public IPage<SysDictType> listTypes(IPage<SysDictType> page, String dictName, String dictType) {
        LambdaQueryWrapper<SysDictType> q = new LambdaQueryWrapper<>();
        q.like(StringUtils.isNotBlank(dictName), SysDictType::getDictName, dictName);
        q.like(StringUtils.isNotBlank(dictType), SysDictType::getDictType, dictType);
        q.orderByDesc(SysDictType::getDictId);
        return dictTypeMapper.selectPage(page, q);
    }

    public List<SysDictType> typesAll() {
        return dictTypeMapper.selectList(null);
    }

    public SysDictType getByIdType(Long id) {
        return dictTypeMapper.selectById(id);
    }

    public IPage<SysDictData> listData(IPage<SysDictData> page, String dictType) {
        LambdaQueryWrapper<SysDictData> q = new LambdaQueryWrapper<>();
        q.eq(StringUtils.isNotBlank(dictType), SysDictData::getDictType, dictType);
        q.orderByAsc(SysDictData::getDictSort);
        return dictDataMapper.selectPage(page, q);
    }

    public List<SysDictData> dataByType(String dictType) {
        LambdaQueryWrapper<SysDictData> q = new LambdaQueryWrapper<>();
        q.eq(SysDictData::getDictType, dictType);
        q.orderByAsc(SysDictData::getDictSort);
        return dictDataMapper.selectList(q);
    }

    public void addType(SysDictType t) {
        dictTypeMapper.insert(t);
    }

    public void updateType(SysDictType t) {
        dictTypeMapper.updateById(t);
    }

    public void deleteTypes(List<Long> ids) {
        dictTypeMapper.deleteBatchIds(ids);
    }

    public void addData(SysDictData d) {
        dictDataMapper.insert(d);
    }

    public void updateData(SysDictData d) {
        dictDataMapper.updateById(d);
    }

    public void deleteData(List<Long> ids) {
        dictDataMapper.deleteBatchIds(ids);
    }
}
