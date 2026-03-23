package com.example.demo.Service.impl;

import ch.qos.logback.core.util.FileUtil;
import com.example.demo.DTO.AnswerItem;
import com.example.demo.DTO.SurveyStatsResponse;
import com.example.demo.Eception.BusinessException;
import com.example.demo.Eception.GlobalExceptionHandler;
import com.example.demo.Service.ResponseService;
import com.example.demo.Utils.FileUtils;
import com.example.demo.entity.Question;
import com.example.demo.entity.Survey;
import com.example.demo.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResponseServiceImpl implements ResponseService {

    private final SurveyRepository surveyRepository;
    private final DepartmentRepository departmentRepository;
    private final ResponseRepository responseRepository;
    private final ResponseAnswerRepository answerRepository;
    private final ResponseFileRepository fileRepository;
    private final QuestionRepository       questionRepository;
    private final EmployeeRepository       employeeRepository;
    private final FileUtils fileUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(Integer surveyId,
                       String empNo,
                       String name,
                       Integer departmentId,
                       List<AnswerItem> answers,
                       Map<String, MultipartFile> files) {

        // ── 1. 校验问卷状态 ───────────────────────────────────────────────
        Survey survey = surveyRepository.findById(surveyId);
        if (survey == null || survey.getStatus() != 1) {
            throw BusinessException.forbidden("问卷不存在或未开放");
        }
        LocalDateTime now = LocalDateTime.now();
        if (survey.getEndsAt() != null && now.isAfter(survey.getEndsAt())) {
            throw BusinessException.forbidden("问卷已截止，无法提交");
        }

        // ── 2. 校验部门是否存在 ───────────────────────────────────────────
        var dept = departmentRepository.findById(departmentId);
        if (dept == null) {
            throw BusinessException.badRequest("所选党组织不存在");
        }

        // ── 3. 防重复提交 ─────────────────────────────────────────────────
        if (responseRepository.existsByEmpNo(surveyId, empNo)) {
            throw BusinessException.conflict("您已提交过本次问卷，请勿重复填写");
        }

        // ── 4. 必答题校验 ─────────────────────────────────────────────────
        validateRequiredAnswers(surveyId, answers);

        // ── 5. 写入答卷主表 ───────────────────────────────────────────────
        Long responseId = responseRepository.insert(
                surveyId, departmentId, empNo, name, dept.getName(), getClientIp(), now
        );

        // ── 6. 写入答题明细 ───────────────────────────────────────────────
        if (answers != null && !answers.isEmpty()) {
            List<ResponseAnswerRepository.AnswerRow> rows = answers.stream()
                    .map(a -> {
                        ResponseAnswerRepository.AnswerRow row = new ResponseAnswerRepository.AnswerRow();
                        row.setQuestionId(a.getQuestionId());
                        row.setOptionId(a.getOptionId());
                        row.setAnswerText(a.getAnswerText() != null ? a.getAnswerText().trim() : null);
                        return row;
                    })
                    .collect(Collectors.toList());
            answerRepository.batchInsert(responseId, rows);
        }

        // ── 7. 处理附件文件 ───────────────────────────────────────────────
        // fieldName 约定：file_{questionId}
        if (files != null) {
            files.forEach((fieldName, file) -> {
                if (file == null || file.isEmpty()) return;
                Integer questionId = parseQuestionId(fieldName);
                if (questionId == null) return;

                String storagePath = fileUtil.save(file);
                fileRepository.insert(
                        responseId, questionId,
                        file.getOriginalFilename(),
                        storagePath,
                        file.getContentType(),
                        file.getSize(),
                        now
                );
            });
        }

        // ── 8. 自动录入花名册（INSERT IGNORE，已存在则跳过） ────────────
        employeeRepository.insertIgnore(empNo, name, departmentId);

        log.info("[submit] surveyId={} empNo={} responseId={}", surveyId, empNo, responseId);
    }

    @Override
    public SurveyStatsResponse getStats(Integer surveyId) {
        Survey survey = surveyRepository.findById(surveyId);
        if (survey == null) {
            throw BusinessException.notFound("问卷不存在");
        }

        int total = responseRepository.countBySurveyId(surveyId);
        List<Question> questions = questionRepository.findBySurveyId(surveyId);

        List<SurveyStatsResponse.QuestionStats> statsList = questions.stream()
                .filter(q -> !"file".equals(q.getType()))
                .map(q -> buildQuestionStats(q, surveyId, total))
                .collect(Collectors.toList());

        SurveyStatsResponse resp = new SurveyStatsResponse();
        resp.setSurveyId(surveyId);
        resp.setSurveyTitle(survey.getTitle());
        resp.setTotalResponses(total);
        resp.setQuestions(statsList);
        return resp;
    }

    // ── 私有辅助方法 ──────────────────────────────────────────────────────

    private void validateRequiredAnswers(Integer surveyId, List<AnswerItem> answers) {
        List<Question> required = questionRepository.findRequiredBySurveyId(surveyId);

        Set<Integer> answeredIds = (answers == null) ? Set.of() :
                answers.stream()
                        .filter(a -> a.getOptionId() != null
                                || (a.getAnswerText() != null && !a.getAnswerText().isBlank()))
                        .map(AnswerItem::getQuestionId)
                        .collect(Collectors.toSet());

        for (Question q : required) {
            if (!answeredIds.contains(q.getId())) {
                throw BusinessException.badRequest(
                        String.format("第 %d 题为必答题，请完成后再提交", q.getSortOrder())
                );
            }
        }
    }

    private SurveyStatsResponse.QuestionStats buildQuestionStats(
            Question q, Integer surveyId, int total) {

        SurveyStatsResponse.QuestionStats qs = new SurveyStatsResponse.QuestionStats();
        qs.setQuestionId(q.getId());
        qs.setSortOrder(q.getSortOrder());
        qs.setContent(q.getContent());
        qs.setType(q.getType());

        if ("radio".equals(q.getType())) {
            List<SurveyStatsResponse.OptionStats> optionStats =
                    answerRepository.statByQuestion(surveyId, q.getId()).stream()
                            .map(s -> {
                                SurveyStatsResponse.OptionStats os = new SurveyStatsResponse.OptionStats();
                                os.setOptionId(s.getOptionId());
                                os.setOptionContent(s.getOptionContent());
                                os.setCount(s.getCount());
                                os.setPercentage(total > 0
                                        ? String.format("%.1f%%", s.getCount() * 100.0 / total)
                                        : "0.0%");
                                return os;
                            })
                            .collect(Collectors.toList());
            qs.setOptions(optionStats);

        } else if ("text".equals(q.getType())) {
            qs.setTextAnswers(answerRepository.findTextAnswers(surveyId, q.getId()));
        }

        return qs;
    }

    private String getClientIp() {
        try {
            HttpServletRequest req = ((ServletRequestAttributes)
                    Objects.requireNonNull(RequestContextHolder.getRequestAttributes()))
                    .getRequest();
            String ip = req.getHeader("X-Forwarded-For");
            if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
                ip = req.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
                ip = req.getRemoteAddr();
            }
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip != null ? ip.substring(0, Math.min(ip.length(), 45)) : "";
        } catch (Exception e) {
            return "";
        }
    }

    private Integer parseQuestionId(String fieldName) {
        try {
            return Integer.parseInt(fieldName.replace("file_", ""));
        } catch (NumberFormatException e) {
            log.warn("[submit] unrecognized file field: {}", fieldName);
            return null;
        }
    }
}

