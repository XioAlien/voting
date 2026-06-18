package com.vote.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OptionRequest {
    @NotBlank(message = "选项内容不能为空")
    @Size(max = 200, message = "选项内容不能超过200个字符")
    private String text;

    @Max(value = 100, message = "选项最大分值不能超过100")
    private Integer maxScore = 100;
}
