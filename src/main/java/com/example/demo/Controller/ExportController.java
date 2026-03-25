package com.example.demo.Controller;

import com.example.demo.Service.ExportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }


    /**
     * GET /api/surveys/{surveyId}/export/responses?deptId=1
     * 按部门导出答卷明细，不传 deptId 则导出全部
     */
    @GetMapping("/surveys/{surveyId}/export/responses")
    public void exportResponses(@PathVariable Integer surveyId,
                                @RequestParam(required = false) Integer deptId,
                                HttpServletResponse response) {
        exportService.exportResponses(surveyId, deptId, response);
    }

    /**
     * GET /api/surveys/{surveyId}/export/stats?deptId=1
     * 按部门导出统计汇总，不传 deptId 则统计全部
     */
    @GetMapping("/surveys/{surveyId}/export/stats")
    public void exportStats(@PathVariable Integer surveyId,
                            @RequestParam(required = false) Integer deptId,
                            HttpServletResponse response) {
        exportService.exportStats(surveyId, deptId, response);
    }
}

