package com.example.admin.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 公告：本工程用作"业务模块"演示（出入库等扩展可参照此模式）
 */
@Data
@TableName("sys_notice")
public class SysNotice implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long noticeId;
    private String noticeTitle;
    private String noticeType; // 1通知 2公告
    private String noticeContent;
    private String status;
    private String createBy;
    private Date createTime;
    private String remark;
}
