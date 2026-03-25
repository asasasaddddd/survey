package com.example.demo.Service;

import jakarta.servlet.http.HttpServletResponse;

public interface ExportService {

    /**
     * 导出某问卷的所有答卷汇总为 Excel
     */
    void exportResponses(Integer surveyId, Integer departmentId, HttpServletResponse response);

    /**
     * 导出某问卷各题选项统计为 Excel
     */
    void exportStats(Integer surveyId, Integer departmentId, HttpServletResponse response);
}