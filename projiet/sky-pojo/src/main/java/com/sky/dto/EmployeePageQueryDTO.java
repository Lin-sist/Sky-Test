package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class EmployeePageQueryDTO implements Serializable {

    // 员工姓名
    private String name;

    // 账号状态 0-禁用 1-启用（Integer 而非 int，不传时为 null，动态 SQL 条件不生效）
    private Integer status;

    // 页码
    private int page;

    // 每页显示记录数
    private int pageSize;

}
