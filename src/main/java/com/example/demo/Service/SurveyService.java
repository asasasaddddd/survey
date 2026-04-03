package com.example.demo.Service;

import com.example.demo.DTO.SurveyResponse;
import com.example.demo.entity.Department;

import java.util.List;

public interface SurveyService {

    List<Department> listDepartments();

    SurveyResponse getSurveyDetail(Integer surveyId, Integer deptId);
}
