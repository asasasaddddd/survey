package com.example.demo.entity;


import lombok.Data;

@Data
public class QuestionOption {
    private Integer id;
    private Integer questionId;
    private Integer sortOrder;
    private String content;
}
