package com.example.admin.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("sys_oper_log")
public class SysOperLog implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long operId;
    private String title;
    private Integer businessType; // 0其它 1新增 2修改 3删除
    private String method;
    private String requestMethod;
    private String operatorType;
    private String operName;
    private String operUrl;
    private String operIp;
    private String operParam;
    private String jsonResult;
    private Integer status; // 0成功 1失败
    private String errorMsg;
    private Date operTime;
}
