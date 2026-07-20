package com.example.admin.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 路由元信息（侧边栏展示）
 */
@Data
public class MetaVO {
    private String title;
    private String icon;
    private List<String> perms;
}
