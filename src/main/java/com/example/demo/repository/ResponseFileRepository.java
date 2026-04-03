package com.example.demo.repository;

import com.example.demo.entity.ResponseFile;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ResponseFileRepository {

    private final JdbcTemplate jdbc;

    private final RowMapper<ResponseFile> ROW_MAPPER = (rs, i) -> {
        ResponseFile f = new ResponseFile();
        f.setId(rs.getLong("id"));
        f.setResponseId(rs.getLong("response_id"));
        f.setQuestionId(rs.getInt("question_id"));
        f.setOriginalName(rs.getString("original_name"));
        f.setStoragePath(rs.getString("storage_path"));
        f.setMimeType(rs.getString("mime_type"));
        f.setFileSize(rs.getLong("file_size"));
        f.setUploadedAt(rs.getTimestamp("uploaded_at").toLocalDateTime());
        return f;
    };

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

    public List<ResponseFile> findBySurveyId(Integer surveyId) {
        return jdbc.query(
                "SELECT f.* FROM response_files f " +
                "JOIN responses r ON f.response_id = r.id " +
                "WHERE r.survey_id = ?",
                ROW_MAPPER, surveyId
        );
    }

    public List<ResponseFile> findBySurveyIdAndDeptId(Integer surveyId, Integer deptId) {
        return jdbc.query(
                "SELECT f.* FROM response_files f " +
                "JOIN responses r ON f.response_id = r.id " +
                "WHERE r.survey_id = ? AND r.department_id = ?",
                ROW_MAPPER, surveyId, deptId
        );
    }
}
