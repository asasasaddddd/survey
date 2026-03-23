package com.example.demo.repository;

import com.example.demo.entity.Department;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class DepartmentRepository {
    private final JdbcTemplate jdbc;

    private final RowMapper<Department> ROW_MAPPER = (rs, i) -> {
        Department d = new Department();
        d.setId(rs.getInt("id"));
        d.setName(rs.getString("name"));
        d.setSortOrder(rs.getInt("sort_order"));
        return d;
    };

    public List<Department> findAll() {
        return jdbc.query(
                "SELECT id, name, sort_order FROM departments ORDER BY sort_order",
                ROW_MAPPER
        );
    }

    public Department findById(Integer id) {
        List<Department> list = jdbc.query(
                "SELECT id, name, sort_order FROM departments WHERE id = ?",
                ROW_MAPPER, id
        );
        return list.isEmpty() ? null : list.get(0);
    }
}