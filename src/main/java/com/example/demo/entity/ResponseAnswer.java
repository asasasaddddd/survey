package com.example.demo.entity;

import lombok.Data;

@Data
public class ResponseAnswer {
    private Long id;
    private Long responseId;
    private Integer questionId;
    private Integer optionId;
    private String answerText;
}
