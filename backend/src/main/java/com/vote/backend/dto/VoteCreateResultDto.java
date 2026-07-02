package com.vote.backend.dto;

import lombok.Data;

@Data
public class VoteCreateResultDto {
  private VoteDto vote;
  private String accessType;
  private VoteInviteDto invite;
}
