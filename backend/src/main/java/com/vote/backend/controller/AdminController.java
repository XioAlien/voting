package com.vote.backend.controller;

import com.vote.backend.dto.AdminConfirmRequest;
import com.vote.backend.dto.AdminDashboardDto;
import com.vote.backend.dto.AdminTerminateVoteRequest;
import com.vote.backend.dto.AdminUserBatchRequest;
import com.vote.backend.dto.AdminUserDto;
import com.vote.backend.dto.AdminUserPageDto;
import com.vote.backend.dto.AdminUserRoleUpdateRequest;
import com.vote.backend.dto.AdminUserStatusUpdateRequest;
import com.vote.backend.dto.AdminUserUpdateRequest;
import com.vote.backend.dto.AdminVoteRuleUpdateRequest;
import com.vote.backend.dto.ApiResponse;
import com.vote.backend.dto.VoteDto;
import com.vote.backend.entity.Vote;
import com.vote.backend.security.UserPrincipal;
import com.vote.backend.service.AdminConfirmationService;
import com.vote.backend.service.AdminService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

  private final AdminService adminService;
  private final AdminConfirmationService adminConfirmationService;

  public AdminController(AdminService adminService, AdminConfirmationService adminConfirmationService) {
    this.adminService = adminService;
    this.adminConfirmationService = adminConfirmationService;
  }

  @GetMapping("/dashboard")
  public ApiResponse<AdminDashboardDto> getDashboard(@AuthenticationPrincipal UserPrincipal principal) {
    return ApiResponse.success("获取成功", adminService.getDashboard(principal));
  }

  @PostMapping({ "/confirm", "/confirmations" })
  public ApiResponse<Map<String, String>> issueConfirmToken(
      @AuthenticationPrincipal UserPrincipal principal,
      @RequestBody AdminConfirmRequest request) {
    String token = adminConfirmationService.issueToken(principal, request);
    return ApiResponse.success("确认令牌签发成功", Map.of("token", token));
  }

  @GetMapping("/users")
  public ApiResponse<AdminUserPageDto> getUsers(
      @AuthenticationPrincipal UserPrincipal principal,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String keyword) {
    return ApiResponse.success("获取成功", adminService.getUsers(principal, page, size, keyword));
  }

  @RequestMapping(value = "/users/{userId}", method = { RequestMethod.PUT, RequestMethod.PATCH })
  public ApiResponse<AdminUserDto> updateUser(
      @AuthenticationPrincipal UserPrincipal principal,
      @PathVariable Long userId,
      @RequestBody(required = false) AdminUserUpdateRequest request) {
    return ApiResponse.success("更新成功", adminService.updateUser(principal, userId, request));
  }

  @RequestMapping(value = "/users/{userId}/role", method = { RequestMethod.PUT, RequestMethod.PATCH })
  public ApiResponse<AdminUserDto> updateUserRole(
      @AuthenticationPrincipal UserPrincipal principal,
      @PathVariable Long userId,
      @RequestBody(required = false) AdminUserRoleUpdateRequest request) {
    return ApiResponse.success("更新成功", adminService.updateUserRole(principal, userId, request));
  }

  @RequestMapping(value = "/users/{userId}/status", method = { RequestMethod.PUT, RequestMethod.PATCH })
  public ApiResponse<AdminUserDto> updateUserStatus(
      @AuthenticationPrincipal UserPrincipal principal,
      @PathVariable Long userId,
      @RequestBody(required = false) AdminUserStatusUpdateRequest request) {
    return ApiResponse.success("更新成功", adminService.updateUserStatus(principal, userId, request));
  }

  @DeleteMapping("/users")
  public ApiResponse<Void> deleteUsers(
      @AuthenticationPrincipal UserPrincipal principal,
      @RequestBody(required = false) AdminUserBatchRequest request) {
    adminService.deleteUsers(principal, request);
    return ApiResponse.success("删除成功", null);
  }

  @RequestMapping(value = "/votes/{voteId}/rule", method = { RequestMethod.PUT, RequestMethod.PATCH })
  public ApiResponse<VoteDto> updateVoteRule(
      @AuthenticationPrincipal UserPrincipal principal,
      @PathVariable Long voteId,
      @RequestBody(required = false) AdminVoteRuleUpdateRequest request) {
    Vote vote = adminService.updateVoteRule(principal, voteId, request);
    return ApiResponse.success("更新成功", toVoteDto(vote));
  }

  @PostMapping("/votes/{voteId}/terminate")
  public ApiResponse<VoteDto> terminateVote(
      @AuthenticationPrincipal UserPrincipal principal,
      @PathVariable Long voteId,
      @RequestBody(required = false) AdminTerminateVoteRequest request) {
    Vote vote = adminService.terminateVote(principal, voteId, request);
    return ApiResponse.success("终止成功", toVoteDto(vote));
  }

  private VoteDto toVoteDto(Vote vote) {
    VoteDto dto = new VoteDto();
    dto.setId(vote.getId());
    dto.setTitle(vote.getTitle());
    dto.setDescription(vote.getDescription());
    dto.setType(vote.getType());
    dto.setStartTime(vote.getStartTime());
    dto.setEndTime(vote.getEndTime());
    dto.setParticipants(0);

    LocalDateTime now = LocalDateTime.now();
    if (vote.getEndTime() != null && now.isAfter(vote.getEndTime())) {
      dto.setStatus("closed");
    } else {
      dto.setStatus(Boolean.TRUE.equals(vote.getIsActive()) ? "active" : "closed");
    }
    return dto;
  }
}
