package com.example.demo.Service;

import com.example.demo.DTO.VerifyEmployeeRequest;
import com.example.demo.DTO.VerifyEmployeeResponse;

public interface EmployeeService {

    /**
     * 校验员工身份
     * - 花名册中有该工号：严格比对姓名和部门
     * - 花名册中无该工号：跳过校验，提交时自动录入
     * - 返回该工号是否已提交过指定问卷
     */
    VerifyEmployeeResponse verify(VerifyEmployeeRequest req, Integer surveyId);
}