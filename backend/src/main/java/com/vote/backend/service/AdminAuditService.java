package com.vote.backend.service;

import com.vote.backend.controller.ApiException;
import com.vote.backend.dto.AdminAuditLogDto;
import com.vote.backend.entity.AdminAuditLog;
import com.vote.backend.repository.AdminAuditLogRepository;
import com.vote.backend.security.UserPrincipal;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AdminAuditService {

  private final AdminAuditLogRepository adminAuditLogRepository;

  public AdminAuditService(AdminAuditLogRepository adminAuditLogRepository) {
    this.adminAuditLogRepository = adminAuditLogRepository;
  }

  public AdminAuditLog record(
      UserPrincipal operator,
      String action,
      String targetType,
      String targetId,
      String detail,
      boolean confirmed) {
    if (operator == null) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "未登录");
    }
    return record(operator.getId(), extractRole(operator), action, targetType, targetId, detail, confirmed);
  }

  public AdminAuditLog record(
      Long operatorId,
      String operatorRole,
      String action,
      String targetType,
      String targetId,
      String detail,
      boolean confirmed) {
    if (operatorId == null) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "操作人不能为空");
    }
    if (!StringUtils.hasText(action)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "审计动作不能为空");
    }
    if (!StringUtils.hasText(targetType)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "审计目标类型不能为空");
    }

    AdminAuditLog log = new AdminAuditLog();
    log.setOperatorId(operatorId);
    log.setOperatorRole(StringUtils.hasText(operatorRole) ? operatorRole.trim() : "USER");
    log.setAction(action.trim().toUpperCase());
    log.setTargetType(targetType.trim().toUpperCase());
    log.setTargetId(StringUtils.hasText(targetId) ? targetId.trim() : null);
    log.setDetail(StringUtils.hasText(detail) ? detail.trim() : null);
    log.setConfirmed(confirmed);
    return adminAuditLogRepository.save(log);
  }

  public List<AdminAuditLogDto> getRecentLogs(int limit) {
    int safeLimit = Math.max(1, Math.min(limit, 200));
    return adminAuditLogRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
        .limit(safeLimit)
        .map(this::toDto)
        .toList();
  }

  public AdminAuditLogDto toDto(AdminAuditLog log) {
    AdminAuditLogDto dto = new AdminAuditLogDto();
    dto.setId(log.getId());
    dto.setOperatorId(log.getOperatorId());
    dto.setOperatorRole(log.getOperatorRole());
    dto.setAction(log.getAction());
    dto.setTargetType(log.getTargetType());
    dto.setTargetId(log.getTargetId());
    dto.setDetail(log.getDetail());
    dto.setConfirmed(log.getConfirmed());
    dto.setCreatedAt(log.getCreatedAt());
    return dto;
  }

  private String extractRole(UserPrincipal principal) {
    return principal.getAuthorities().stream()
        .map(authority -> authority.getAuthority())
        .filter(StringUtils::hasText)
        .findFirst()
        .map(authority -> authority.startsWith("ROLE_") ? authority.substring(5) : authority)
        .orElse("USER");
  }
}
