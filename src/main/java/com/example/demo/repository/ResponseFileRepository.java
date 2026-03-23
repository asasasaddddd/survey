package com.example.demo.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class ResponseFileRepository {

    private final JdbcTemplate jdbc;

    public void insert(Long responseId, Integer questionId, String originalName,
                       String storagePath, String mimeType, Long fileSize,
                       LocalDateTime uploadedAt) {
        jdbc.update(
                "INSERT INTO response_files " +
                        "(response_id, question_id, original_name, storage_path, mime_type, file_size, uploaded_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                responseId, questionId, originalName, storagePath, mimeType, fileSize,
                Timestamp.valueOf(uploadedAt)
        );
    }
}
