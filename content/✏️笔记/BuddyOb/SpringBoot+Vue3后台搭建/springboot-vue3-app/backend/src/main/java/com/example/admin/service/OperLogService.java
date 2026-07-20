package com.example.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.admin.common.utils.StringUtils;
import com.example.admin.domain.SysOperLog;
import com.example.admin.mapper.SysOperLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 操作日志（由 @Log 切面写入，支持查询/删除/清空）
 */
@Service
@RequiredArgsConstructor
public class OperLogService {

    private final SysOperLogMapper operLogMapper;

    public IPage<SysOperLog> list(IPage<SysOperLog> page, String title, String operName, Integer status) {
        LambdaQueryWrapper<SysOperLog> q = new LambdaQueryWrapper<>();
        q.like(StringUtils.isNotBlank(title), SysOperLog::getTitle, title);
        q.like(StringUtils.isNotBlank(operName), SysOperLog::getOperName, operName);
        q.eq(status != null, SysOperLog::getStatus, status);
        q.orderByDesc(SysOperLog::getOperId);
        return operLogMapper.selectPage(page, q);
    }

    public void removeByIds(List<Long> ids) {
        operLogMapper.deleteBatchIds(ids);
    }

    /** 清空日志（操作日志不可删=仅管理员可清空，这里按需求提供清空能力） */
    public void clean() {
        operLogMapper.delete(new LambdaQueryWrapper<SysOperLog>());
    }
}
