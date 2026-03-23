package com.example.demo.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Employee {
    private Integer id;
    private String empNo;
    private String name;
    private Integer departmentId;
    private Integer isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}