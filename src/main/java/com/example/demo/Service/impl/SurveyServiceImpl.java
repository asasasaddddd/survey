package com.example.demo.Service.impl;

import com.example.demo.DTO.SurveyResponse;
import com.example.demo.Eception.BusinessException;
import com.example.demo.Service.SurveyService;
import com.example.demo.entity.Department;
import com.example.demo.entity.Question;
import com.example.demo.entity.QuestionOption;
import com.example.demo.entity.Survey;
import com.example.demo.repository.DepartmentRepository;
import com.example.demo.repository.QuestionOptionRepository;
import com.example.demo.repository.QuestionRepository;
import com.example.demo.repository.SurveyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SurveyServiceImpl implements SurveyService {

    private final DepartmentRepository departmentRepository;
    private final SurveyRepository surveyRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;

    @Override
    public List<Department> listDepartments() {
        return departmentRepository.findAll();
    }

    @Override
    public SurveyResponse getSurveyDetail(Integer surveyId) {
        // 1. 查询并校验问卷
        Survey survey = surveyRepository.findById(surveyId);
        if (survey == null) {
            throw BusinessException.notFound("问卷不存在");
        }
        if (survey.getStatus() != 1) {
            throw BusinessException.forbidden("问卷未开放");
        }
        LocalDateTime now = LocalDateTime.now();
        if (survey.getStartsAt() != null && now.isBefore(survey.getStartsAt())) {
            throw BusinessException.forbidden("问卷尚未开始");
        }
        if (survey.getEndsAt() != null && now.isAfter(survey.getEndsAt())) {
            throw BusinessException.forbidden("问卷已截止");
        }

        // 2. 查询题目
        List<Question> questions = questionRepository.findBySurveyId(surveyId);
        if (questions.isEmpty()) {
            throw BusinessException.notFound("问卷暂无题目");
        }

        // 3. 批量查询选项，一次 IN 查询避免 N+1
        List<Integer> questionIds = questions.stream()
                .map(Question::getId).collect(Collectors.toList());

        Map<Integer, List<SurveyResponse.OptionDTO>> optionMap = optionRepository
                .findByQuestionIds(questionIds)
                .stream()
                .collect(Collectors.groupingBy(
                        QuestionOption::getQuestionId,
                        Collectors.mapping(opt -> {
                            SurveyResponse.OptionDTO dto = new SurveyResponse.OptionDTO();
                            dto.setId(opt.getId());
                            dto.setContent(opt.getContent());
                            return dto;
                        }, Collectors.toList())
                ));

        // 4. 按 section 分组（LinkedHashMap 保持原始顺序）
        Map<String, List<SurveyResponse.QuestionDTO>> sectionMap = new LinkedHashMap<>();
        for (Question q : questions) {
            SurveyResponse.QuestionDTO qDto = new SurveyResponse.QuestionDTO();
            qDto.setId(q.getId());
            qDto.setSortOrder(q.getSortOrder());
            qDto.setType(q.getType());
            qDto.setContent(q.getContent());
            qDto.setIsRequired(q.getIsRequired() == 1);
            qDto.setOptions(optionMap.getOrDefault(q.getId(), List.of()));
            sectionMap.computeIfAbsent(q.getSection(), k -> new ArrayList<>()).add(qDto);
        }

        // 5. 组装返回体
        List<SurveyResponse.SectionDTO> sections = sectionMap.entrySet().stream()
                .map(e -> {
                    SurveyResponse.SectionDTO sec = new SurveyResponse.SectionDTO();
                    sec.setName(e.getKey());
                    sec.setQuestions(e.getValue());
                    return sec;
                }).collect(Collectors.toList());

        SurveyResponse resp = new SurveyResponse();
        resp.setId(survey.getId());
        resp.setTitle(survey.getTitle());
        resp.setDescription(survey.getDescription());
        resp.setSections(sections);
        return resp;
    }
}
