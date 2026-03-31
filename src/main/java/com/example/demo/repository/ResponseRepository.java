package com.example.demo.repository;

import com.example.demo.entity.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Repository
@RequiredArgsConstructor
public class ResponseRepository {

    private final JdbcTemplate jdbc;

    private final RowMapper<Response> ROW_MAPPER = (rs, i) -> {
        Response r = new Response();
        r.setId(rs.getLong("id"));
        r.setSurveyId(rs.getInt("survey_id"));
        r.setDepartmentId(rs.getInt("department_id"));
        r.setDeptName(rs.getString("dept_name"));
        r.setSubmittedAt(rs.getTimestamp("submitted_at").toLocalDateTime());
        return r;
    };

    /**
     * 插入答卷主表，返回自增主键
     */
    public Long insert(Integer surveyId, Integer departmentId,
                       String empNo, String empName, String deptName,
                       String ip, LocalDateTime submittedAt) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO responses (survey_id, department_id, emp_no, emp_name, dept_name, ip, submitted_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setInt(1, surveyId);
            ps.setInt(2, departmentId);
            ps.setString(3, empNo);
            ps.setString(4, empName);
            ps.setString(5, deptName);
            ps.setString(6, ip);
            ps.setTimestamp(7, Timestamp.valueOf(submittedAt));
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    /**
     * 插入答卷主表（匿名模式），返回自增主键
     */
    public Long insertAnonymous(Integer surveyId, Integer departmentId,
                                String deptName, String ip, String fingerprint,
                                LocalDateTime submittedAt) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO responses (survey_id, department_id, dept_name, ip, device_fingerprint, submitted_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setInt(1, surveyId);
            ps.setInt(2, departmentId);
            ps.setString(3, deptName);
            ps.setString(4, ip);
            ps.setString(5, fingerprint);
            ps.setTimestamp(6, Timestamp.valueOf(submittedAt));
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public boolean existsByFingerprint(Integer surveyId, String fingerprint) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM responses WHERE survey_id = ? AND device_fingerprint = ?",
                Integer.class, surveyId, fingerprint
        );
        return count != null && count > 0;
    }

    public int countBySurveyId(Integer surveyId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM responses WHERE survey_id = ?",
                Integer.class, surveyId
        );
        return count != null ? count : 0;
    }

    public List<Response> findBySurveyId(Integer surveyId) {
        return jdbc.query(
                "SELECT id, survey_id, department_id, dept_name, submitted_at " +
                        "FROM responses WHERE survey_id = ? ORDER BY submitted_at DESC",
                ROW_MAPPER, surveyId
        );
    }
    public List<Response> findBySurveyAndDept(Integer surveyId, Integer departmentId) {
        return jdbc.query(
                "SELECT id, survey_id, department_id, dept_name, submitted_at " +
                        "FROM responses WHERE survey_id = ? AND department_id = ? ORDER BY submitted_at DESC",
                ROW_MAPPER, surveyId, departmentId
        );
    }
}