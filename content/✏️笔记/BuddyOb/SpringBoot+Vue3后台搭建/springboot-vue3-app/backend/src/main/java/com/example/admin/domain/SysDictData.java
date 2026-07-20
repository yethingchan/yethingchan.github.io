package com.example.admin.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("sys_dict_data")
public class SysDictData implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long dictCode;
    private String dictType;
    private String dictLabel;
    private String dictValue;
    private Integer dictSort;
    private String status;
    private String remark;
}
