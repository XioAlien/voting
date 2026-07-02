package com.vote.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class VoteCreateRequest {
  @NotBlank(message = "标题不能为空")
  @Size(max = 100, message = "标题不能超过100个字符")
  private String title;

  @Size(max = 1000, message = "描述不能超过1000个字符")
  private String description;
  private LocalDateTime startTime;
  private LocalDateTime endTime;

  @Pattern(regexp = "CHOICE|SLIDER", message = "投票类型不合法")
  private String type = "CHOICE";

  @Pattern(regexp = "PUBLIC|INVITE", message = "访问方式不合法")
  private String accessType = "PUBLIC";

  @Min(value = 1, message = "最少选择数不能小于1")
  private Integer minChoices = 1;

  @Min(value = 1, message = "最多选择数不能小于1")
  @Max(value = 100, message = "最多选择数不能超过100")
  private Integer maxChoices = 1;
  private Boolean forceAllOptions = false;
  private Boolean allowCustomOptions = false;
  @Min(value = 1, message = "最大成员数必须大于0")
  @Max(value = 100000, message = "最大成员数不能超过100000")
  private Integer inviteMaxMembers;
  private LocalDateTime inviteExpiresAt;

  @NotEmpty(message = "至少需要一个选项")
  @Valid
  private List<OptionRequest> options;
}
