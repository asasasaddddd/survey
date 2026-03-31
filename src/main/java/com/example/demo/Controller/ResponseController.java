package com.example.demo.Controller;
import com.example.demo.DTO.SurveyStatsResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.demo.DTO.AnswerItem;
import com.example.demo.DTO.Result;
import com.example.demo.Eception.BusinessException;
import com.example.demo.Service.ResponseService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ResponseController {
    private final ResponseService responseService;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    /**
     * POST /api/surveys/{surveyId}/responses
     * 提交答卷（匿名模式），Content-Type: multipart/form-data
     *
     * 表单字段：
     *   departmentId 部门 ID（从 URL 参数获取）
     *   answers      JSON 字符串 [{"questionId":1,"optionId":5}, ...]
     *   file_{qId}   附件文件（可选）
     */
    @PostMapping("/surveys/{surveyId}/responses")
    public Result<Void> submit(@PathVariable Integer surveyId,
                               HttpServletRequest request) {

        // ── 解析表单字段 ──────────────────────────────────────────────────
        String deptIdStr   = require(request, "departmentId", "请选择所属党组织");
        String answersJson = require(request, "answers",      "answers 不能为空");

        Integer departmentId;
        try {
            departmentId = Integer.valueOf(deptIdStr.trim());
        } catch (NumberFormatException e) {
            throw BusinessException.badRequest("departmentId 格式错误");
        }

        List<AnswerItem> answers;
        try {
            answers = objectMapper.readValue(answersJson,
                    new TypeReference<List<AnswerItem>>() {});
        } catch (Exception e) {
            throw BusinessException.badRequest("answers 格式错误，应为 JSON 数组");
        }

        // ── 收集附件（fieldName 以 file_ 开头） ──────────────────────────
        Map<String, MultipartFile> fileMap = new HashMap<>();
        if (request instanceof MultipartHttpServletRequest multipart) {
            multipart.getFileMap().forEach((fieldName, file) -> {
                if (fieldName.startsWith("file_") && !file.isEmpty()) {
                    fileMap.put(fieldName, file);
                }
            });
        }

        responseService.submit(surveyId, departmentId, answers, fileMap);

        return Result.ok("提交成功，感谢您的参与！");
    }

    /**
     * GET /api/surveys/{surveyId}/stats
     * 查询问卷统计数据（各题选项分布 + 文字答案）
     */
    @GetMapping("/surveys/{surveyId}/stats")
    public Result<SurveyStatsResponse> getStats(@PathVariable Integer surveyId) {
        return Result.ok(responseService.getStats(surveyId));
    }

    // ── 工具方法 ──────────────────────────────────────────────────────────

    private String require(HttpServletRequest request, String name, String errMsg) {
        String value = request.getParameter(name);
        if (value == null || value.isBlank()) {
            throw BusinessException.badRequest(errMsg);
        }
        return value;
    }
}
