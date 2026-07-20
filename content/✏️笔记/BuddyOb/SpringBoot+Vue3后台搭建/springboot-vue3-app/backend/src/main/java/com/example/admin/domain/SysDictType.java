package com.example.admin.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("sys_dict_type")
public class SysDictType implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long dictId;
    private String dictName;
    private String dictType; // 字典类型（代码）
    private String status;
    private String remark;
}
