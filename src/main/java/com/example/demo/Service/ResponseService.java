package com.example.demo.Service;
import com.example.demo.DTO.AnswerItem;
import com.example.demo.DTO.SurveyStatsResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface ResponseService {

    void submit(Integer surveyId,
                Integer departmentId,
                List<AnswerItem> answers,
                Map<String, MultipartFile> files);

    SurveyStatsResponse getStats(Integer surveyId);
}

