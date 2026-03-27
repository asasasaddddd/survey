package com.example.demo.Controller;

import com.example.demo.DTO.Result;
import com.example.demo.DTO.VerifyEmployeeRequest;
import com.example.demo.DTO.VerifyEmployeeResponse;
import com.example.demo.Service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class EmployeeController {
    private final EmployeeService employeeService;
    /**
     * POST /api/surveys/{surveyId}/employee/verify
     * 进入问卷前校验工号身份并检查是否已提交
     */
    @PostMapping("/surveys/{surveyId}/employee/verify")
    public Result<VerifyEmployeeResponse> verify(@PathVariable Integer surveyId,
                                                   @Valid @RequestBody VerifyEmployeeRequest req) {
        VerifyEmployeeResponse response = employeeService.verify(req, surveyId);
        return Result.ok(response);
    }
}
