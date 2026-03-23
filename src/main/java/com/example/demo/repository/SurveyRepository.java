package com.example.demo.repository;

import com.example.demo.entity.Survey;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SurveyRepository {

    private final JdbcTemplate jdbc;

    private final RowMapper<Survey> ROW_MAPPER = (rs, i) -> {
        Survey s = new Survey();
        s.setId(rs.getInt("id"));
        s.setTitle(rs.getString("title"));
        s.setDescription(rs.getString("description"));
        s.setStatus(rs.getInt("status"));
        // starts_at / ends_at 可为 NULL
        s.setStartsAt(rs.getTimestamp("starts_at") != null
                ? rs.getTimestamp("starts_at").toLocalDateTime() : null);
        s.setEndsAt(rs.getTimestamp("ends_at") != null
                ? rs.getTimestamp("ends_at").toLocalDateTime() : null);
        return s;
    };

    public Survey findById(Integer id) {
        List<Survey> list = jdbc.query(
                "SELECT id, title, description, status, starts_at, ends_at FROM surveys WHERE id = ? LIMIT 1",
                ROW_MAPPER, id
        );
        return list.isEmpty() ? null : list.get(0);
    }
}