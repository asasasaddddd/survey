package com.example.demo.entity;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Survey {
    private Integer id;
    private String title;
    private String description;
    private Integer year;
    private Integer status;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
