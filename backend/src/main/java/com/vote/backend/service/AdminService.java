package com.vote.backend.service;

import com.vote.backend.controller.ApiException;
import com.vote.backend.dto.AdminDashboardDto;
import com.vote.backend.dto.AdminTerminateVoteRequest;
import com.vote.backend.dto.AdminUserBatchRequest;
import com.vote.backend.dto.AdminUserDto;
import com.vote.backend.dto.AdminUserPageDto;
import com.vote.backend.dto.AdminUserRoleUpdateRequest;
import com.vote.backend.dto.AdminUserStatusUpdateRequest;
import com.vote.backend.dto.AdminUserUpdateRequest;
import com.vote.backend.dto.AdminVoteRuleUpdateRequest;
import com.vote.backend.entity.User;
import com.vote.backend.entity.Vote;
import com.vote.backend.repository.AdminAuditLogRepository;
import com.vote.backend.repository.UserRepository;
import com.vote.backend.repository.VoteRecordRepository;
import com.vote.backend.repository.VoteRepository;
import com.vote.backend.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class AdminService {

  private final UserRepository userRepository;
  private final VoteRepository voteRepository;
  private final VoteRecordRepository voteRecordRepository;
  private final AdminAuditLogRepository adminAuditLogRepository;
  private final AdminAuditService adminAuditService;
  private final AdminConfirmationService adminConfirmationService;

  public AdminService(
      UserRepository userRepository,
      VoteRepository voteRepository,
      VoteRecordRepository voteRecordRepository,
      AdminAuditLogRepository adminAuditLogRepository,
      AdminAuditService adminAuditService,
      AdminConfirmationService adminConfirmationService) {
    this.userRepository = userRepository;
    this.voteRepository = voteRepository;
    this.voteRecordRepository = voteRecordRepository;
    this.adminAuditLogRepository = adminAuditLogRepository;
    this.adminAuditService = adminAuditService;
    this.adminConfirmationService = adminConfirmationService;
  }

  public AdminDashboardDto getDashboard(UserPrincipal principal) {
    requireAdmin(principal);

    long totalUsers = userRepository.count();
    long totalVotes = voteRepository.count();
    long activeVotes = voteRepository.findAll().stream()
        .filter(vote -> Boolean.TRUE.equals(vote.getIsActive()))
        .filter(vote -> vote.getEndTime() == null || !LocalDateTime.now().isAfter(vote.getEndTime()))
        .count();

    // 当前 User 实体没有 disabled/deleted 语义，因此 activeUsers 暂按总用户数回填。
    return new AdminDashboardDto(
        totalUsers,
        totalUsers,
        totalVotes,
        activeVotes,
        voteRecordRepository.count(),
        adminAuditLogRepository.count());
  }

  public AdminUserPageDto getUsers(UserPrincipal principal, int page, int size, String keyword) {
    requireAdmin(principal);

    int safePage = Math.max(page, 0);
    int safeSize = Math.max(1, Math.min(size, 100));
    String normalizedKeyword = keyword == null ? null : keyword.trim().toLowerCase(Locale.ROOT);

    List<AdminUserDto> allItems = userRepository.findAll().stream()
        .filter(user -> matchesKeyword(user, normalizedKeyword))
        .sorted(Comparator
            .comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(User::getId, Comparator.nullsLast(Comparator.reverseOrder())))
        .map(this::toDto)
        .toList();

    int fromIndex = Math.min(safePage * safeSize, allItems.size());
    int toIndex = Math.min(fromIndex + safeSize, allItems.size());

    return new AdminUserPageDto(allItems.subList(fromIndex, toIndex), allItems.size(), safePage, safeSize);
  }

  @Transactional
  public AdminUserDto updateUser(UserPrincipal principal, Long userId, AdminUserUpdateRequest request) {
    requireAdmin(principal);
    User user = getUser(userId);

    if (request != null && request.getUsername() != null) {
      String username = normalizeRequired(request.getUsername(), "用户名不能为空");
      if (!username.equals(user.getUsername()) && Boolean.TRUE.equals(userRepository.existsByUsername(username))) {
        throw new ApiException(HttpStatus.CONFLICT, "用户名已存在");
      }
      user.setUsername(username);
    }

    if (request != null && request.getEmail() != null) {
      String email = normalizeRequired(request.getEmail(), "邮箱不能为空");
      if (!email.equals(user.getEmail()) && Boolean.TRUE.equals(userRepository.existsByEmail(email))) {
        throw new ApiException(HttpStatus.CONFLICT, "邮箱已存在");
      }
      user.setEmail(email);
    }

    User saved = userRepository.save(user);
    adminAuditService.record(principal, "UPDATE_USER", "USER", String.valueOf(userId),
        "更新用户基础信息", false);
    return toDto(saved);
  }

  @Transactional
  public AdminUserDto updateUserRole(UserPrincipal principal, Long userId, AdminUserRoleUpdateRequest request) {
    requireAdmin(principal);
    adminConfirmationService.validateToken(principal,
        request == null ? null : request.getConfirmToken(), "UPDATE_USER_ROLE", String.valueOf(userId));

    User user = getUser(userId);
    String role = normalizeRole(request == null ? null : request.getRole());
    if (principal.getId().equals(userId) && !"ADMIN".equals(role)) {
      throw new ApiException(HttpStatus.CONFLICT, "不能移除自己的管理员权限");
    }

    user.setRole(role);
    User saved = userRepository.save(user);
    adminAuditService.record(principal, "UPDATE_USER_ROLE", "USER", String.valueOf(userId),
        "更新用户角色为 " + role, true);
    return toDto(saved);
  }

  public AdminUserDto updateUserStatus(UserPrincipal principal, Long userId, AdminUserStatusUpdateRequest request) {
    requireAdmin(principal);
    adminConfirmationService.validateToken(principal,
        request == null ? null : request.getConfirmToken(), "UPDATE_USER_STATUS", String.valueOf(userId));

    if (request != null && Boolean.TRUE.equals(request.getDisabled())) {
      throw new ApiException(HttpStatus.CONFLICT, "当前用户实体未提供禁用状态，暂不支持禁用账号");
    }

    User user = getUser(userId);
    adminAuditService.record(principal, "UPDATE_USER_STATUS", "USER", String.valueOf(userId),
        "当前模型不支持禁用字段，状态更新按无操作处理", true);
    return toDto(user);
  }

  public void deleteUsers(UserPrincipal principal, AdminUserBatchRequest request) {
    requireAdmin(principal);
    adminConfirmationService.validateToken(principal,
        request == null ? null : request.getConfirmToken(), "DELETE_USERS", buildBatchTarget(request));
    throw new ApiException(HttpStatus.CONFLICT, "当前用户实体未提供 deleted 语义，暂不支持批量删除用户");
  }

  @Transactional
  public Vote updateVoteRule(UserPrincipal principal, Long voteId, AdminVoteRuleUpdateRequest request) {
    requireAdmin(principal);
    adminConfirmationService.validateToken(principal,
        request == null ? null : request.getConfirmToken(), "UPDATE_VOTE_RULE", String.valueOf(voteId));

    Vote vote = getVote(voteId);
    if (request == null) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "请求参数不能为空");
    }

    if (request.getTitle() != null) {
      vote.setTitle(normalizeRequired(request.getTitle(), "投票标题不能为空"));
    }
    if (request.getDescription() != null) {
      vote.setDescription(request.getDescription().trim());
    }
    if (request.getType() != null) {
      vote.setType(normalizeVoteType(request.getType()));
    }
    if (request.getMinChoices() != null) {
      vote.setMinChoices(request.getMinChoices());
    }
    if (request.getMaxChoices() != null) {
      vote.setMaxChoices(request.getMaxChoices());
    }
    if (request.getForceAllOptions() != null) {
      vote.setForceAllOptions(request.getForceAllOptions());
    }
    if (request.getAllowCustomOptions() != null) {
      vote.setAllowCustomOptions(request.getAllowCustomOptions());
    }
    if (request.getEndTime() != null) {
      vote.setEndTime(request.getEndTime());
    }

    validateVoteRule(vote);

    Vote saved = voteRepository.save(vote);
    adminAuditService.record(principal, "UPDATE_VOTE_RULE", "VOTE", String.valueOf(voteId),
        "更新投票规则", true);
    return saved;
  }

  @Transactional
  public Vote terminateVote(UserPrincipal principal, Long voteId, AdminTerminateVoteRequest request) {
    requireAdmin(principal);
    adminConfirmationService.validateToken(principal,
        request == null ? null : request.getConfirmToken(), "TERMINATE_VOTE", String.valueOf(voteId));

    Vote vote = getVote(voteId);
    vote.setIsActive(false);
    if (vote.getEndTime() == null || vote.getEndTime().isAfter(LocalDateTime.now())) {
      vote.setEndTime(LocalDateTime.now());
    }

    Vote saved = voteRepository.save(vote);
    adminAuditService.record(principal, "TERMINATE_VOTE", "VOTE", String.valueOf(voteId),
        "终止投票", true);
    return saved;
  }

  private boolean matchesKeyword(User user, String keyword) {
    if (!StringUtils.hasText(keyword)) {
      return true;
    }
    return user.getUsername().toLowerCase(Locale.ROOT).contains(keyword)
        || user.getEmail().toLowerCase(Locale.ROOT).contains(keyword);
  }

  private AdminUserDto toDto(User user) {
    AdminUserDto dto = new AdminUserDto();
    dto.setId(user.getId());
    dto.setUsername(user.getUsername());
    dto.setEmail(user.getEmail());
    dto.setRole(user.getRole());
    dto.setBuiltinAdmin(Boolean.TRUE.equals(user.getIsBuiltinAdmin()));
    dto.setDisabled(false);
    dto.setDeleted(false);
    dto.setMustChangePassword(Boolean.TRUE.equals(user.getMustChangePassword()));
    dto.setLastLoginAt(user.getLastLoginAt());
    dto.setCreatedAt(user.getCreatedAt());
    dto.setUpdatedAt(user.getUpdatedAt());
    return dto;
  }

  private void requireAdmin(UserPrincipal principal) {
    if (principal == null) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "未登录");
    }
    if (!adminConfirmationService.isAdmin(principal)) {
      throw new ApiException(HttpStatus.FORBIDDEN, "无管理员权限");
    }
  }

  private User getUser(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "用户不存在"));
  }

  private Vote getVote(Long voteId) {
    return voteRepository.findById(voteId)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "投票不存在"));
  }

  private String normalizeRequired(String value, String errorMessage) {
    if (!StringUtils.hasText(value)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, errorMessage);
    }
    return value.trim();
  }

  private String normalizeRole(String role) {
    String normalized = normalizeRequired(role, "角色不能为空").toUpperCase(Locale.ROOT);
    if (!List.of("USER", "ADMIN").contains(normalized)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "角色不合法");
    }
    return normalized;
  }

  private String normalizeVoteType(String voteType) {
    String normalized = normalizeRequired(voteType, "投票类型不能为空").toUpperCase(Locale.ROOT);
    if (!List.of("CHOICE", "SLIDER").contains(normalized)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "投票类型不合法");
    }
    return normalized;
  }

  private void validateVoteRule(Vote vote) {
    if ("CHOICE".equals(vote.getType())) {
      if (vote.getMinChoices() == null || vote.getMaxChoices() == null
          || vote.getMinChoices() < 1 || vote.getMaxChoices() < vote.getMinChoices()) {
        throw new ApiException(HttpStatus.BAD_REQUEST, "投票选项数量不合法");
      }
    }
  }

  private String buildBatchTarget(AdminUserBatchRequest request) {
    if (request == null || request.getUserIds() == null || request.getUserIds().isEmpty()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "用户列表不能为空");
    }
    return request.getUserIds().stream()
        .sorted()
        .map(String::valueOf)
        .reduce((left, right) -> left + "," + right)
        .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "用户列表不能为空"));
  }
}
