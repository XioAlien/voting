package com.vote.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserPageDto {
  private List<AdminUserDto> items;
  private long total;
  private int page;
  private int size;
}
