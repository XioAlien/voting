package com.vote.backend.controller;

import com.vote.backend.dto.ApiResponse;
import com.vote.backend.dto.OptionRequest;
import com.vote.backend.dto.VoteCreateRequest;
import com.vote.backend.dto.VoteCreateResultDto;
import com.vote.backend.dto.VoteDto;
import com.vote.backend.dto.VoteDetailDto;
import com.vote.backend.dto.VoteInviteDto;
import com.vote.backend.dto.VoteInviteSettingsRequest;
import com.vote.backend.dto.VoteJoinRequest;
import com.vote.backend.dto.VoteJoinResultDto;
import com.vote.backend.dto.VoteOptionDto;
import com.vote.backend.dto.VoteSubmitRequest;
import com.vote.backend.service.VoteInviteService;
import com.vote.backend.service.VoteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.vote.backend.security.UserPrincipal;
import java.util.List;

@RestController
@RequestMapping("/api/votes")
public class VoteController {

  @Autowired
  private VoteService voteService;

  @Autowired
  private VoteInviteService voteInviteService;

  private UserPrincipal getCurrentPrincipal() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      return null;
    }
    Object principal = auth.getPrincipal();
    if (principal instanceof UserPrincipal user) {
      return user;
    }
    return null;
  }

  private Long getCurrentUserId() {
    UserPrincipal principal = getCurrentPrincipal();
    return principal == null ? null : principal.getId();
  }

  @GetMapping
  public ApiResponse<List<VoteDto>> getAllVotes() {
    List<VoteDto> votes = voteService.getAllVotes(getCurrentPrincipal());
    return ApiResponse.success("获取成功", votes);
  }

  @GetMapping("/{id}/results")
  public ApiResponse<VoteDetailDto> getVoteDetail(@PathVariable Long id) {
    VoteDetailDto detail = voteService.getVoteDetail(id, getCurrentPrincipal());
    return ApiResponse.success("获取成功", detail);
  }

  @PostMapping
  public ApiResponse<VoteCreateResultDto> createVote(@Valid @RequestBody VoteCreateRequest request) {
    Long userId = getCurrentUserId();
    if (userId == null) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "未登录");
    }
    VoteCreateResultDto result = voteService.createVoteWithInvite(userId, request);
    return ApiResponse.success("创建成功", result);
  }

  @PostMapping("/{voteId}/options")
  public ApiResponse<VoteOptionDto> addOption(@PathVariable Long voteId,
      @Valid @RequestBody OptionRequest request) {
    VoteOptionDto optionDto = voteService.addCustomOption(getCurrentPrincipal(), voteId, request);
    return ApiResponse.success("添加选项成功", optionDto);
  }

  @PostMapping("/{voteId}/join")
  public ApiResponse<VoteJoinResultDto> joinVote(
      @PathVariable Long voteId,
      @RequestBody(required = false) VoteJoinRequest request) {
    VoteJoinResultDto result = voteInviteService.joinVote(getCurrentPrincipal(), voteId, request);
    return ApiResponse.success("加入成功", result);
  }

  @PostMapping("/join-by-invite")
  public ApiResponse<VoteJoinResultDto> joinVoteByInvite(
      @RequestBody(required = false) VoteJoinRequest request) {
    VoteJoinResultDto result = voteInviteService.joinVoteByInviteCode(getCurrentPrincipal(), request);
    return ApiResponse.success("加入成功", result);
  }

  @RequestMapping(value = "/{voteId}/leave", method = { RequestMethod.POST, RequestMethod.DELETE })
  public ApiResponse<Void> leaveVote(@PathVariable Long voteId) {
    voteInviteService.leaveVote(getCurrentPrincipal(), voteId);
    return ApiResponse.success("退出成功", null);
  }

  @GetMapping("/{voteId}/invite")
  public ApiResponse<VoteInviteDto> getInvite(@PathVariable Long voteId) {
    VoteInviteDto invite = voteInviteService.getInvite(getCurrentPrincipal(), voteId);
    return ApiResponse.success("获取成功", invite);
  }

  @PostMapping("/{voteId}/invite/reset")
  public ApiResponse<VoteInviteDto> resetInvite(@PathVariable Long voteId) {
    VoteInviteDto invite = voteInviteService.resetInviteCode(getCurrentPrincipal(), voteId);
    return ApiResponse.success("重置成功", invite);
  }

  @RequestMapping(value = "/{voteId}/invite/settings", method = { RequestMethod.PUT, RequestMethod.PATCH })
  public ApiResponse<VoteInviteDto> updateInviteSettings(
      @PathVariable Long voteId,
      @RequestBody(required = false) VoteInviteSettingsRequest request) {
    VoteInviteDto invite = voteInviteService.updateSettings(getCurrentPrincipal(), voteId, request);
    return ApiResponse.success("更新成功", invite);
  }

  @PostMapping("/{voteId}/vote")
  public ApiResponse<String> vote(@PathVariable Long voteId, @Valid @RequestBody VoteSubmitRequest request) {
    boolean success = voteService.castVote(getCurrentPrincipal(), voteId, request);
    if (success) {
      return ApiResponse.success("投票成功", null);
    } else {
      throw new ApiException(HttpStatus.CONFLICT, "您已投过票或投票无效");
    }
  }
}
