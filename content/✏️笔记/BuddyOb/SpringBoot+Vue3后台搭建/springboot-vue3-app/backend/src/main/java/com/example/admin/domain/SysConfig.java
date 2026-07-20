package com.example.admin.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("sys_config")
public class SysConfig implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long configId;
    private String configName;
    private String configKey;
    private String configValue;
    private String configType; // Y=系统内置 N=自定义
    private String remark;
}
