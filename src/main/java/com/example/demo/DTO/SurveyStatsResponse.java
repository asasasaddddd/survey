package com.example.demo.DTO;

import lombok.Data;

import java.util.List;

@Data
public class SurveyStatsResponse {

    private Integer surveyId;
    private String surveyTitle;
    private Integer totalResponses;
    private List<QuestionStats> questions;

    @Data
    public static class QuestionStats {
        private Integer questionId;
        private Integer sortOrder;
        private String content;
        private String type;
        /** 单选题各选项统计 */
        private List<OptionStats> options;
        /** 文字题所有回答列表 */
        private List<String> textAnswers;
    }

    @Data
    public static class OptionStats {
        private Integer optionId;
        private String optionContent;
        private Long count;
        /** 占比，如 "42.5%" */
        private String percentage;
    }
}
