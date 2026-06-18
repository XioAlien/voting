package com.vote.backend.dto;

import lombok.Data;

import java.util.List;

@Data
public class AdminUserBatchRequest {
  private List<Long> userIds;
  private String confirmToken;
}
