package com.example.demo.DTO;

import lombok.Data;

import java.util.List;

@Data
public class SurveyResponse {

    private Integer id;
    private String title;
    private String description;
    private String departmentName;
    private List<SectionDTO> sections;

    @Data
    public static class SectionDTO {
        private String name;
        private List<QuestionDTO> questions;
    }

    @Data
    public static class QuestionDTO {
        private Integer id;
        private Integer sortOrder;
        private String type;
        private String content;
        private Boolean isRequired;
        private List<OptionDTO> options;
    }

    @Data
    public static class OptionDTO {
        private Integer id;
        private String content;
    }
}