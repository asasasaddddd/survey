package com.example.demo.Service.impl;

import com.example.demo.DTO.VerifyEmployeeRequest;
import com.example.demo.Eception.BusinessException;
import com.example.demo.Service.EmployeeService;
import com.example.demo.entity.Employee;
import com.example.demo.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;

    @Override
    public void verify(VerifyEmployeeRequest req) {
        Employee emp = employeeRepository.findByEmpNo(req.getEmpNo());

        // 花名册中不存在该工号：跳过校验
        if (emp == null) return;

        if (emp.getIsActive() == 0) {
            throw BusinessException.forbidden("该工号已离职，无法填写问卷");
        }
        if (!emp.getName().equals(req.getName())) {
            throw BusinessException.badRequest("工号与姓名不匹配，请核实后重试");
        }
        if (!emp.getDepartmentId().equals(req.getDepartmentId())) {
            throw BusinessException.badRequest("工号与所选党组织不匹配，请核实后重试");
        }
    }
}
