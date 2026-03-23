package com.example.demo.repository;

import com.example.demo.entity.Question;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class QuestionRepository {

    private final JdbcTemplate jdbc;

    private final RowMapper<Question> ROW_MAPPER = (rs, i) -> {
        Question q = new Question();
        q.setId(rs.getInt("id"));
        q.setSurveyId(rs.getInt("survey_id"));
        q.setSection(rs.getString("section"));
        q.setSortOrder(rs.getInt("sort_order"));
        q.setType(rs.getString("type"));
        q.setContent(rs.getString("content"));
        q.setIsRequired(rs.getInt("is_required"));
        return q;
    };

    public List<Question> findBySurveyId(Integer surveyId) {
        return jdbc.query(
                "SELECT id, survey_id, section, sort_order, type, content, is_required " +
                        "FROM questions WHERE survey_id = ? ORDER BY sort_order",
                ROW_MAPPER, surveyId
        );
    }

    public List<Question> findRequiredBySurveyId(Integer surveyId) {
        return jdbc.query(
                "SELECT id, survey_id, section, sort_order, type, content, is_required " +
                        "FROM questions WHERE survey_id = ? AND is_required = 1 AND type != 'file'",
                ROW_MAPPER, surveyId
        );
    }
}
