package com.vote.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminDashboardDto {
  private long totalUsers;
  private long activeUsers;
  private long totalVotes;
  private long activeVotes;
  private long totalVoteRecords;
  private long totalAuditLogs;
}
