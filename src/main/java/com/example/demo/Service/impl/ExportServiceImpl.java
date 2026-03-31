package com.example.demo.Service.impl;

import com.example.demo.Eception.BusinessException;
import com.example.demo.Service.ExportService;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExportServiceImpl implements ExportService {

    private final SurveyRepository surveyRepository;
    private final QuestionRepository questionRepository;
    private final ResponseRepository       responseRepository;
    private final ResponseAnswerRepository answerRepository;
    private final DepartmentRepository departmentRepository;

    public ExportServiceImpl(SurveyRepository surveyRepository,
                             QuestionRepository questionRepository,
                             ResponseRepository responseRepository,
                             ResponseAnswerRepository answerRepository, DepartmentRepository departmentRepository) {
        this.surveyRepository  = surveyRepository;
        this.questionRepository = questionRepository;
        this.responseRepository = responseRepository;
        this.answerRepository   = answerRepository;
        this.departmentRepository = departmentRepository;
    }

    // ── 导出原始答卷明细 ──────────────────────────────────────────────────

    @Override
    public void exportResponses(Integer surveyId, Integer deptId,
                                HttpServletResponse response) {
        Survey survey       = getSurveyOrThrow(surveyId);
        Department dept     = deptId != null ? getDeptOrThrow(deptId) : null;
        List<Question> questions = questionRepository.findBySurveyId(surveyId);

        // 按部门过滤答卷
        List<Response> responses = responseRepository.findBySurveyId(surveyId)
                .stream()
                .filter(r -> deptId == null || deptId.equals(r.getDepartmentId()))
                .collect(Collectors.toList());

        String sheetName = dept != null ? dept.getName() : "全部";
        String filename  = "答卷明细_" + survey.getTitle()
                + (dept != null ? "_" + dept.getName() : "") + ".xlsx";

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(sheetName);

            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle dataStyle   = createDataStyle(wb);
            CellStyle wrapStyle   = createWrapStyle(wb);

            // 题目列（跳过文件题）
            List<Question> exportQs = questions.stream()
                    .filter(q -> !"file".equals(q.getType()))
                    .collect(Collectors.toList());

            // 表头
            List<String> headers = new ArrayList<>(
                    List.of("序号", "所属党组织", "提交时间"));
            exportQs.forEach(q -> headers.add("第" + q.getSortOrder() + "题"));

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, i < 3 ? 4000 : 8000);
            }

            // 数据行
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            int rowNum = 1;
            for (Response resp : responses) {
                List<ResponseAnswer> answers = answerRepository.findByResponseId(resp.getId());
                Map<Integer, String> answerMap = answers.stream()
                        .collect(Collectors.toMap(
                                ResponseAnswer::getQuestionId,
                                a -> a.getAnswerText() != null ? a.getAnswerText() : "",
                                (a, b) -> a
                        ));

                Row row = sheet.createRow(rowNum++);
                int col = 0;
                setCell(row, col++, String.valueOf(rowNum - 1), dataStyle);
                setCell(row, col++, resp.getDeptName(), dataStyle);
                setCell(row, col++,
                        resp.getSubmittedAt() != null
                                ? resp.getSubmittedAt().format(fmt) : "",
                        dataStyle);
                for (Question q : exportQs) {
                    setCell(row, col++,
                            answerMap.getOrDefault(q.getId(), ""), wrapStyle);
                }
            }

            sheet.createFreezePane(3, 1);
            writeResponse(response, wb, filename);
        } catch (IOException e) {
            throw new BusinessException(500, "导出失败，请稍后重试");
        }
    }

    // ── 导出统计汇总 ──────────────────────────────────────────────────────

    @Override
    public void exportStats(Integer surveyId, Integer departments, HttpServletResponse response) {
        Survey survey = getSurveyOrThrow(surveyId);
        List<Question> questions = questionRepository.findBySurveyId(surveyId);
        Department department = departments != null ? getDeptOrThrow(departments) : null;
        int total = (int) responseRepository.findBySurveyId(surveyId).stream()
                .filter(r -> departments == null || departments.equals(r.getDepartmentId()))
                .count();

        String deptLabel = department != null ? department.getName() : "全部党组织";
        String filename  = "统计汇总_" + survey.getTitle()
                + (department != null ? "_" + department.getName() : "") + ".xlsx";

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("统计汇总");

            CellStyle titleStyle  = createTitleStyle(wb);
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle dataStyle   = createDataStyle(wb);
            CellStyle pctStyle    = createDataStyle(wb);

            // 设置列宽
            sheet.setColumnWidth(0, 1500);   // 题号
            sheet.setColumnWidth(1, 14000);  // 题目内容
            sheet.setColumnWidth(2, 10000);  // 选项
            sheet.setColumnWidth(3, 3000);   // 人数
            sheet.setColumnWidth(4, 3000);   // 占比

            // 大标题
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(survey.getTitle() + " — " + deptLabel +  " 统计汇总");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));
            titleRow.setHeightInPoints(28);

            // 总人数行
            Row totalRow = sheet.createRow(1);
            Cell totalCell = totalRow.createCell(0);
            totalCell.setCellValue("总填写人数：" + total + " 人");
            totalCell.setCellStyle(dataStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 4));

            // 表头
            Row headerRow = sheet.createRow(2);
            String[] cols = {"题号", "题目内容", "选项", "人数", "占比"};
            for (int i = 0; i < cols.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(cols[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 3;
            for (Question q : questions) {
                if ("file".equals(q.getType())) continue;

                if ("radio".equals(q.getType())) {
                    // 单选题：每个选项一行
                    List<ResponseAnswerRepository.OptionStat> stats =
                            departments == null ?
                            answerRepository.statByQuestion(surveyId, q.getId())
                            :answerRepository.statByQuestionAndDept(surveyId, q.getId(), departments);

                    int startRow = rowNum;
                    if (stats.isEmpty()) {
                        // 没有人答这道题
                        Row row = sheet.createRow(rowNum++);
                        setCell(row, 0, String.valueOf(q.getSortOrder()), dataStyle);
                        setCell(row, 1, q.getContent(), dataStyle);
                        setCell(row, 2, "暂无数据", dataStyle);
                        setCell(row, 3, "0", dataStyle);
                        setCell(row, 4, "0.0%", dataStyle);
                    } else {
                        for (ResponseAnswerRepository.OptionStat s : stats) {
                            Row row = sheet.createRow(rowNum++);
                            setCell(row, 0, String.valueOf(q.getSortOrder()), dataStyle);
                            setCell(row, 1, q.getContent(), dataStyle);
                            setCell(row, 2, s.getOptionContent(), dataStyle);
                            setCell(row, 3, String.valueOf(s.getCount()), dataStyle);
                            String pct = total > 0
                                    ? String.format("%.1f%%", s.getCount() * 100.0 / total)
                                    : "0.0%";
                            setCell(row, 4, pct, pctStyle);
                        }
                    }
                    // 合并题号和题目列
                    if (rowNum - startRow > 1) {
                        sheet.addMergedRegion(new CellRangeAddress(startRow, rowNum - 1, 0, 0));
                        sheet.addMergedRegion(new CellRangeAddress(startRow, rowNum - 1, 1, 1));
                    }

                } else if ("text".equals(q.getType())) {
                    // 文字题：列出所有回答
                    List<String> textAnswers =
                            departments == null ?
                            answerRepository.findTextAnswers(surveyId, q.getId())
                            : answerRepository.findTextAnswersByDept(surveyId, q.getId(), departments);

                    int startRow = rowNum;
                    if (textAnswers.isEmpty()) {
                        Row row = sheet.createRow(rowNum++);
                        setCell(row, 0, String.valueOf(q.getSortOrder()), dataStyle);
                        setCell(row, 1, q.getContent(), dataStyle);
                        setCell(row, 2, "暂无回答", dataStyle);
                        setCell(row, 3, "", dataStyle);
                        setCell(row, 4, "", dataStyle);
                    } else {
                        for (String text : textAnswers) {
                            Row row = sheet.createRow(rowNum++);
                            setCell(row, 0, String.valueOf(q.getSortOrder()), dataStyle);
                            setCell(row, 1, q.getContent(), dataStyle);
                            setCell(row, 2, text, dataStyle);
                            setCell(row, 3, "", dataStyle);
                            setCell(row, 4, "", dataStyle);
                        }
                    }
                    if (rowNum - startRow > 1) {
                        sheet.addMergedRegion(new CellRangeAddress(startRow, rowNum - 1, 0, 0));
                        sheet.addMergedRegion(new CellRangeAddress(startRow, rowNum - 1, 1, 1));
                    }
                }
            }

            writeResponse(response, wb, filename);
        } catch (IOException e) {
            throw new BusinessException(500, "导出失败，请稍后重试");
        }
    }

    // ── 样式工厂 ──────────────────────────────────────────────────────────

    private CellStyle createTitleStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 14);
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        return s;
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(s);
        return s;
    }

    private CellStyle createDataStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setWrapText(true);
        setBorder(s);
        return s;
    }

    private CellStyle createWrapStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setWrapText(true);
        s.setVerticalAlignment(VerticalAlignment.TOP);
        setBorder(s);
        return s;
    }

    private void setBorder(CellStyle s) {
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
    }

    // ── 工具方法 ──────────────────────────────────────────────────────────

    private void setCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void writeResponse(HttpServletResponse response,
                               Workbook wb, String filename) throws IOException {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replace("+", "%20");
        response.setContentType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + encoded);
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
        wb.write(response.getOutputStream());
    }

    private Survey getSurveyOrThrow(Integer surveyId) {
        Survey survey = surveyRepository.findById(surveyId);
        if (survey == null) throw BusinessException.notFound("问卷不存在");
        return survey;
    }
    private Department getDeptOrThrow(Integer deptId) {
        Department d = departmentRepository.findById(deptId);
        if (d == null) throw BusinessException.notFound("党组织不存在，deptId=" + deptId);
        return d;
    }
}
