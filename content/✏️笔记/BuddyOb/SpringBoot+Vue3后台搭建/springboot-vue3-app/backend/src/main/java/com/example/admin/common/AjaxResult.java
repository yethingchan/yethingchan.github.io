package com.example.admin.common;

import java.io.Serializable;
import java.util.List;

/**
 * 统一返回结果：前端 request.js 只认 code。
 * code=200 成功；非 200 业务异常，message 弹错；401 跳登录。
 */
public class AjaxResult implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int SUCCESS = 200;
    public static final int WARN = 601;
    public static final int ERROR = 500;

    private int code;
    private String msg;
    private Object data;

    /** 分页：行集合 */
    private List<?> rows;
    /** 分页：总数 */
    private Long total;

    public AjaxResult() {
    }

    public AjaxResult(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public AjaxResult(int code, String msg, Object data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static AjaxResult success() {
        return new AjaxResult(SUCCESS, "操作成功");
    }

    public static AjaxResult success(String msg) {
        return new AjaxResult(SUCCESS, msg);
    }

    public static AjaxResult success(Object data) {
        return new AjaxResult(SUCCESS, "操作成功", data);
    }

    /** 分页成功：前端 el-table 用 rows + total */
    public static AjaxResult success(List<?> rows, Long total) {
        AjaxResult r = new AjaxResult(SUCCESS, "查询成功");
        r.setRows(rows);
        r.setTotal(total);
        return r;
    }

    public static AjaxResult warn(String msg) {
        return new AjaxResult(WARN, msg);
    }

    public static AjaxResult error() {
        return new AjaxResult(ERROR, "操作失败");
    }

    public static AjaxResult error(String msg) {
        return new AjaxResult(ERROR, msg);
    }

    public static AjaxResult error(int code, String msg) {
        return new AjaxResult(code, msg);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public List<?> getRows() {
        return rows;
    }

    public void setRows(List<?> rows) {
        this.rows = rows;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }
}
