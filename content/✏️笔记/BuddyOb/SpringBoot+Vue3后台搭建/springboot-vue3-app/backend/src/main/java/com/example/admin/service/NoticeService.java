package com.example.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.admin.common.utils.StringUtils;
import com.example.admin.domain.SysNotice;
import com.example.admin.mapper.SysNoticeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 公告：本工程"业务模块"演示（出入库等扩展可参照此模式）
 */
@Service
@RequiredArgsConstructor
public class NoticeService {

    private final SysNoticeMapper noticeMapper;

    public IPage<SysNotice> list(IPage<SysNotice> page, String title, String type) {
        LambdaQueryWrapper<SysNotice> q = new LambdaQueryWrapper<>();
        q.like(StringUtils.isNotBlank(title), SysNotice::getNoticeTitle, title);
        q.eq(StringUtils.isNotBlank(type), SysNotice::getNoticeType, type);
        q.orderByDesc(SysNotice::getNoticeId);
        return noticeMapper.selectPage(page, q);
    }

    public SysNotice get(Long id) {
        return noticeMapper.selectById(id);
    }

    public void add(SysNotice n) {
        noticeMapper.insert(n);
    }

    public void update(SysNotice n) {
        noticeMapper.updateById(n);
    }

    public void delete(List<Long> ids) {
        noticeMapper.deleteBatchIds(ids);
    }
}
