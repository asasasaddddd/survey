package com.example.demo.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Response {
    private Long id;
    private Integer surveyId;
    private Integer departmentId;
    private String empNo;
    private String empName;
    private String deptName;
    private LocalDateTime submittedAt;
    private String ip;
}
