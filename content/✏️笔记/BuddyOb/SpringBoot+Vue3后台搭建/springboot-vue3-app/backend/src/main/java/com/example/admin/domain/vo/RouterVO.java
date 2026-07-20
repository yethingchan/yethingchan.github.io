package com.example.admin.domain.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.List;

/**
 * 前端动态路由 VO（与 Vue Router 结构对齐）
 */
@Data
public class RouterVO {
    private Long menuId; // 构建树 + 前端菜单授权树用
    private String name;
    private String path;
    private String component;
    private String redirect;
    private MetaVO meta;
    private List<RouterVO> children;
}
