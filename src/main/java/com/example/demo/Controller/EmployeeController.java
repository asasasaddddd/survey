package com.example.demo.Controller;

import com.example.demo.DTO.Result;
import com.example.demo.DTO.VerifyEmployeeRequest;
import com.example.demo.Service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/employee")
public class EmployeeController {
    private final EmployeeService employeeService;
    /**
     * POST /api/employee/verify
     * 进入问卷前校验工号身份
     */
    @PostMapping("/verify")
    public Result<Void> verify(@Valid @RequestBody VerifyEmployeeRequest req) {
        employeeService.verify(req);
        return Result.ok("验证通过");
    }
}
