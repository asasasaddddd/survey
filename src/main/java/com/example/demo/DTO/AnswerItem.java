package com.example.demo.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AnswerItem {

    @NotNull(message = "questionId 不能为空")
    private Integer questionId;

    /** 单选题传选项 ID */
    private Integer optionId;

    /** 文字题传文本内容 */
    private String answerText;
}