package com.sky.service;

import com.sky.dto.EmployeeLoginDTO;
import com.sky.entity.Employee;
import com.sky.dto.EmployeeDTO;     //EmployeeDTO cannot be resolved to a type错误：需要先导入相应的包。

public interface EmployeeService {

    /**
     * 员工登录
     * @param employeeLoginDTO
     * @return
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);

    void save(EmployeeDTO employeeDTO);     //接口方法默认为public所以不需要额外添加

}
