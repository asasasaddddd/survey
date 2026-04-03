package com.example.demo.Controller;

import com.example.demo.DTO.Result;
import com.example.demo.DTO.SurveyResponse;
import com.example.demo.Service.SurveyService;
import com.example.demo.entity.Department;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SurveyController {
    private final SurveyService surveyService;
    /**
     * GET /api/departments
     * 获取党组织列表，前端下拉用
     */
    @GetMapping("/departments")
    public Result<List<Department>> getDepartments() {
        return Result.ok(surveyService.listDepartments());
    }
    /**
     * GET /api/surveys/{surveyId}
     * 获取问卷详情（含题目和选项，按 section 分组）
     */
    @GetMapping("/surveys/{surveyId}")
    public Result<SurveyResponse> getSurvey(@PathVariable Integer surveyId,
                                            @RequestParam(required = false) Integer deptId) {
        return Result.ok(surveyService.getSurveyDetail(surveyId, deptId));
    }
}
