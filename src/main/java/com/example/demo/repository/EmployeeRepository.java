package com.example.demo.repository;
import com.example.demo.entity.Employee;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class EmployeeRepository {

    private final JdbcTemplate jdbc;

    private final RowMapper<Employee> ROW_MAPPER = (rs, i) -> {
        Employee e = new Employee();
        e.setId(rs.getInt("id"));
        e.setEmpNo(rs.getString("emp_no"));
        e.setName(rs.getString("name"));
        e.setDepartmentId(rs.getInt("department_id"));
        e.setIsActive(rs.getInt("is_active"));
        return e;
    };

    public Employee findByEmpNo(String empNo) {
        List<Employee> list = jdbc.query(
                "SELECT id, emp_no, name, department_id, is_active FROM employees WHERE emp_no = ? LIMIT 1",
                ROW_MAPPER, empNo
        );
        return list.isEmpty() ? null : list.get(0);
    }

    public boolean existsByEmpNo(String empNo) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM employees WHERE emp_no = ?",
                Integer.class, empNo
        );
        return count != null && count > 0;
    }

    /**
     * 首次填写时自动录入花名册，使用 INSERT IGNORE 防并发重复
     */
    public void insertIgnore(String empNo, String name, Integer departmentId) {
        jdbc.update(
                "INSERT IGNORE INTO employees (emp_no, name, department_id, is_active) VALUES (?, ?, ?, 1)",
                empNo, name, departmentId
        );
    }
}