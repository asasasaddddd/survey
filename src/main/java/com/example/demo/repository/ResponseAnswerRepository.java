package com.example.demo.repository;

import com.example.demo.entity.ResponseAnswer;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ResponseAnswerRepository {

    private final JdbcTemplate jdbc;

    /**
     * 批量插入答题明细，一次 INSERT 多行提升效率
     */
    public void batchInsert(Long responseId, List<AnswerRow> rows) {
        if (rows == null || rows.isEmpty()) return;
        for (AnswerRow row : rows) {
            jdbc.update(
                    "INSERT INTO response_answers (response_id, question_id, option_id, answer_text) VALUES (?, ?, ?, ?)",
                    responseId, row.getQuestionId(), row.getOptionId(), row.getAnswerText()
            );
        }
    }

    /**
     * 统计某问卷某题各选项的选择人数
     */
    public List<OptionStat> statByQuestion(Integer surveyId, Integer questionId) {
        return jdbc.query(
                "SELECT ra.option_id AS optionId, qo.content AS optionContent, COUNT(*) AS cnt " +
                        "FROM response_answers ra " +
                        "JOIN responses r ON r.id = ra.response_id " +
                        "JOIN question_options qo ON qo.id = ra.option_id " +
                        "WHERE r.survey_id = ? AND ra.question_id = ? AND ra.option_id IS NOT NULL " +
                        "GROUP BY ra.option_id, qo.content, qo.sort_order " +
                        "ORDER BY qo.sort_order",
                (rs, i) -> {
                    OptionStat s = new OptionStat();
                    s.setOptionId(rs.getInt("optionId"));
                    s.setOptionContent(rs.getString("optionContent"));
                    s.setCount(rs.getLong("cnt"));
                    return s;
                },
                surveyId, questionId
        );
    }
    public List<OptionStat> statByQuestionAndDept(Integer surveyId, Integer questionId, Integer deptId) {
        return jdbc.query(
                "SELECT ra.option_id AS optionId, qo.content AS optionContent, COUNT(*) AS cnt " +
                        "FROM response_answers ra " +
                        "JOIN responses r ON r.id = ra.response_id " +
                        "JOIN question_options qo ON qo.id = ra.option_id " +
                        "WHERE r.survey_id = ? AND ra.question_id = ? AND r.department_id = ? AND ra.option_id IS NOT NULL " +
                        "GROUP BY ra.option_id, qo.content, qo.sort_order ORDER BY qo.sort_order",
                (rs, i) -> {
                    OptionStat s = new OptionStat();
                    s.setOptionId(rs.getInt("optionId"));
                    s.setOptionContent(rs.getString("optionContent"));
                    s.setCount(rs.getLong("cnt"));
                    return s;
                },
                surveyId, questionId, deptId
        );
    }

    /**
     * 查询某题所有文字回答（过滤空值）
     */
    public List<String> findTextAnswers(Integer surveyId, Integer questionId) {
        return jdbc.queryForList(
                "SELECT ra.answer_text FROM response_answers ra " +
                        "JOIN responses r ON r.id = ra.response_id " +
                        "WHERE r.survey_id = ? AND ra.question_id = ? " +
                        "AND ra.answer_text IS NOT NULL AND ra.answer_text != ''",
                String.class, surveyId, questionId
        );
    }
    /** 查询某题某党组织的文字回答 */
    public List<String> findTextAnswersByDept(Integer surveyId, Integer questionId, Integer deptId) {
        return jdbc.queryForList(
                "SELECT ra.answer_text FROM response_answers ra " +
                        "JOIN responses r ON r.id = ra.response_id " +
                        "WHERE r.survey_id = ? AND ra.question_id = ? AND r.department_id = ? " +
                        "AND ra.answer_text IS NOT NULL AND ra.answer_text != ''",
                String.class, surveyId, questionId, deptId
        );
    }

    public List<ResponseAnswer> findByResponseId(Long responseId) {
        return jdbc.query(
                "SELECT id, response_id, question_id, option_id, answer_text FROM response_answers WHERE response_id = ?",
                (rs, i) -> {
                    ResponseAnswer a = new ResponseAnswer();
                    a.setId(rs.getLong("id"));
                    a.setResponseId(rs.getLong("response_id"));
                    a.setQuestionId(rs.getInt("question_id"));
                    a.setOptionId(rs.getObject("option_id") != null ? rs.getInt("option_id") : null);
                    a.setAnswerText(rs.getString("answer_text"));
                    return a;
                },
                responseId
        );
    }

    // ── 内部数据类 ────────────────────────────────────────────────────────

    @Data
    public static class AnswerRow {
        private Integer questionId;
        private Integer optionId;
        private String answerText;
    }

    @Data
    public static class OptionStat {
        private Integer optionId;
        private String optionContent;
        private Long count;
    }
}