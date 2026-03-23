package com.example.demo.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Department {
    private Integer id;
    private String name;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
