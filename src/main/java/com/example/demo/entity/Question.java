package com.example.demo.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Question {
    private Integer id;
    private Integer surveyId;
    private String section;
    private Integer sortOrder;
    private String type;
    private String content;
    private Integer isRequired;
    private LocalDateTime createdAt;
}
