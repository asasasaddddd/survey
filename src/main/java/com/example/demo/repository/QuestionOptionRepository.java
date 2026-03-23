package com.example.demo.repository;

import com.example.demo.entity.QuestionOption;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class QuestionOptionRepository {

    private final JdbcTemplate jdbc;

    private final RowMapper<QuestionOption> ROW_MAPPER = (rs, i) -> {
        QuestionOption o = new QuestionOption();
        o.setId(rs.getInt("id"));
        o.setQuestionId(rs.getInt("question_id"));
        o.setSortOrder(rs.getInt("sort_order"));
        o.setContent(rs.getString("content"));
        return o;
    };

    /**
     * 批量按 questionId 列表查询选项，一次 IN 查询避免 N+1
     */
    public List<QuestionOption> findByQuestionIds(List<Integer> questionIds) {
        if (CollectionUtils.isEmpty(questionIds)) return Collections.emptyList();

        String placeholders = String.join(",", Collections.nCopies(questionIds.size(), "?"));
        String sql = "SELECT id, question_id, sort_order, content FROM question_options " +
                "WHERE question_id IN (" + placeholders + ") ORDER BY question_id, sort_order";
        return jdbc.query(sql, ROW_MAPPER, questionIds.toArray());
    }
}
