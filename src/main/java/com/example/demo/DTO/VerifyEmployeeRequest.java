package com.example.demo.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VerifyEmployeeRequest {

    @NotBlank(message = "工号不能为空")
    private String empNo;

    @NotBlank(message = "姓名不能为空")
    private String name;

    @NotNull(message = "请选择所属党组织")
    private Integer departmentId;
}
