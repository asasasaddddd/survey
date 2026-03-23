package com.example.demo.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ResponseFile {
    private Long id;
    private Long responseId;
    private Integer questionId;
    private String originalName;
    private String storagePath;
    private String mimeType;
    private Long fileSize;
    private LocalDateTime uploadedAt;
}