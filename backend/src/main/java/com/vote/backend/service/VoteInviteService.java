package com.vote.backend.service;

import com.vote.backend.controller.ApiException;
import com.vote.backend.dto.VoteInviteDto;
import com.vote.backend.dto.VoteCreateRequest;
import com.vote.backend.dto.VoteInviteSettingsRequest;
import com.vote.backend.dto.VoteJoinRequest;
import com.vote.backend.dto.VoteJoinResultDto;
import com.vote.backend.entity.User;
import com.vote.backend.entity.Vote;
import com.vote.backend.entity.VoteInvite;
import com.vote.backend.entity.VoteMembership;
import com.vote.backend.repository.UserRepository;
import com.vote.backend.repository.VoteInviteRepository;
import com.vote.backend.repository.VoteMembershipRepository;
import com.vote.backend.repository.VoteRepository;
import com.vote.backend.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

@Service
public class VoteInviteService {

  private static final String INVITE_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

  private final VoteRepository voteRepository;
  private final VoteInviteRepository voteInviteRepository;
  private final VoteMembershipRepository voteMembershipRepository;
  private final UserRepository userRepository;
  private final AdminAuditService adminAuditService;
  private final AdminConfirmationService adminConfirmationService;
  private final int defaultMaxMembers;
  private final int defaultExpireHours;
  private final int inviteCodeLength;
  private final SecureRandom secureRandom = new SecureRandom();

  public VoteInviteService(
      VoteRepository voteRepository,
      VoteInviteRepository voteInviteRepository,
      VoteMembershipRepository voteMembershipRepository,
      UserRepository userRepository,
      AdminAuditService adminAuditService,
      AdminConfirmationService adminConfirmationService,
      @Value("${app.invite.default-max-members:100}") int defaultMaxMembers,
      @Value("${app.invite.default-expire-hours:168}") int defaultExpireHours,
      @Value("${app.invite.code-length:8}") int inviteCodeLength) {
    this.voteRepository = voteRepository;
    this.voteInviteRepository = voteInviteRepository;
    this.voteMembershipRepository = voteMembershipRepository;
    this.userRepository = userRepository;
    this.adminAuditService = adminAuditService;
    this.adminConfirmationService = adminConfirmationService;
    this.defaultMaxMembers = Math.max(1, defaultMaxMembers);
    this.defaultExpireHours = Math.max(1, defaultExpireHours);
    this.inviteCodeLength = Math.max(6, inviteCodeLength);
  }

  @Transactional
  public VoteInvite prepareInviteForCreate(Vote vote, VoteCreateRequest request) {
    VoteInvite invite = new VoteInvite();
    invite.setVote(vote);
    invite.setCodeVersion(1);

    String plainCode = generateInviteCode();
    invite.setCodeHash(hashInviteCode(plainCode));
    invite.setCodeCiphertext(plainCode);

    String accessType = request == null || !StringUtils.hasText(request.getAccessType())
        ? "PUBLIC"
        : normalizeAccessType(request.getAccessType());
    boolean inviteEnabled = "INVITE".equals(accessType);
    invite.setEnabled(inviteEnabled);
    invite.setMaxMembers(resolveMaxMembers(request == null ? null : request.getInviteMaxMembers()));
    invite.setExpiresAt(resolveCreateExpiresAt(request == null ? null : request.getInviteExpiresAt(), inviteEnabled));

    return voteInviteRepository.save(invite);
  }

  public VoteInviteDto toManagerDto(VoteInvite invite) {
    return toDto(invite, true);
  }

  public VoteInviteDto getInvite(UserPrincipal principal, Long voteId) {
    Vote vote = getVote(voteId);
    requireInviteManager(principal, vote);
    VoteInvite invite = voteInviteRepository.findById(voteId).orElseGet(() -> createDefaultInvite(vote));
    return toDto(invite, true);
  }

  @Transactional
  public VoteInviteDto resetInviteCode(UserPrincipal principal, Long voteId) {
    Vote vote = getVote(voteId);
    requireInviteManager(principal, vote);

    VoteInvite invite = voteInviteRepository.findById(voteId).orElseGet(() -> createDefaultInvite(vote));
    String code = generateInviteCode();
    invite.setCodeVersion(invite.getCodeVersion() == null ? 1 : invite.getCodeVersion() + 1);
    invite.setCodeHash(hashInviteCode(code));
    // 当前分支尚未接入独立加密服务，暂将可回显的邀请码保存在 recoverable 字段中供管理员查看。
    invite.setCodeCiphertext(code);
    invite.setResetAt(LocalDateTime.now());
    invite.setResetBy(getUser(principal.getId()));
    if (invite.getEnabled() == null) {
      invite.setEnabled(true);
    }

    VoteInvite saved = voteInviteRepository.save(invite);
    adminAuditService.record(principal, "RESET_INVITE_CODE", "VOTE", String.valueOf(voteId),
        "重置投票邀请码", true);
    return toDto(saved, true);
  }

  @Transactional
  public VoteInviteDto updateSettings(UserPrincipal principal, Long voteId, VoteInviteSettingsRequest request) {
    Vote vote = getVote(voteId);
    requireInviteManager(principal, vote);
    if (request == null) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "请求参数不能为空");
    }

    VoteInvite invite = voteInviteRepository.findById(voteId).orElseGet(() -> createDefaultInvite(vote));

    if (request.getAccessType() != null) {
      String accessType = normalizeAccessType(request.getAccessType());
      invite.setEnabled("INVITE".equals(accessType));
    }
    if (request.getEnabled() != null) {
      invite.setEnabled(request.getEnabled());
    }
    if (request.getExpiresAt() != null) {
      if (request.getExpiresAt().isBefore(LocalDateTime.now())) {
        throw new ApiException(HttpStatus.BAD_REQUEST, "邀请码过期时间不能早于当前时间");
      }
      invite.setExpiresAt(request.getExpiresAt());
    }
    if (request.getMaxMembers() != null) {
      if (request.getMaxMembers() < 1) {
        throw new ApiException(HttpStatus.BAD_REQUEST, "最大成员数必须大于0");
      }
      invite.setMaxMembers(request.getMaxMembers());
    }

    VoteInvite saved = voteInviteRepository.save(invite);
    adminAuditService.record(principal, "UPDATE_INVITE_SETTINGS", "VOTE", String.valueOf(voteId),
        "更新投票邀请码设置", false);
    return toDto(saved, true);
  }

  @Transactional
  public VoteJoinResultDto joinVote(UserPrincipal principal, Long voteId, VoteJoinRequest request) {
    if (principal == null) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "未登录");
    }

    Vote vote = getVote(voteId);
    if (isVoteManager(principal, vote)) {
      return buildJoinResult(voteId, principal.getId().equals(vote.getCreator().getId()) ? "CREATOR" : "ADMIN");
    }

    User user = getUser(principal.getId());
    VoteInvite invite = voteInviteRepository.findById(voteId).orElse(null);
    Optional<VoteMembership> existingMembership = voteMembershipRepository.findByVote_IdAndUser_Id(voteId,
        principal.getId());
    VoteMembership membership = existingMembership.orElseGet(VoteMembership::new);

    if (existingMembership.isPresent() && "ACTIVE".equals(membership.getStatus())) {
      return buildJoinResult(voteId, "MEMBER");
    }

    validateJoinRequest(invite, request);

    if (membership.getId() == null) {
      membership.setVote(vote);
      membership.setUser(user);
    }
    membership.setStatus("ACTIVE");
    membership.setJoinedAt(LocalDateTime.now());
    membership.setLeftAt(null);
    membership.setJoinedCodeVersion(invite == null ? null : invite.getCodeVersion());
    voteMembershipRepository.save(membership);

    return buildJoinResult(voteId, "MEMBER");
  }

  @Transactional
  public VoteJoinResultDto joinVoteByInviteCode(UserPrincipal principal, VoteJoinRequest request) {
    if (principal == null) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "未登录");
    }

    String inviteCode = request == null ? null : request.getInviteCode();
    if (!StringUtils.hasText(inviteCode)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "邀请码不能为空");
    }

    VoteInvite invite = voteInviteRepository.findByCodeHash(hashInviteCode(inviteCode.trim()))
        .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "邀请码无效"));
    Long voteId = resolveVoteId(invite);
    if (voteId == null) {
      throw new ApiException(HttpStatus.NOT_FOUND, "投票不存在");
    }

    return joinVote(principal, voteId, request);
  }

  @Transactional
  public void leaveVote(UserPrincipal principal, Long voteId) {
    if (principal == null) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "未登录");
    }

    VoteMembership membership = voteMembershipRepository.findByVote_IdAndUser_Id(voteId, principal.getId())
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "未加入该投票"));
    if (!"ACTIVE".equals(membership.getStatus())) {
      throw new ApiException(HttpStatus.CONFLICT, "当前未处于已加入状态");
    }

    membership.setStatus("LEFT");
    membership.setLeftAt(LocalDateTime.now());
    voteMembershipRepository.save(membership);
  }

  public boolean canAccessVote(UserPrincipal principal, Long voteId) {
    Vote vote = getVote(voteId);
    VoteInvite invite = voteInviteRepository.findById(voteId).orElse(null);

    if (!requiresInvite(invite)) {
      return true;
    }
    if (principal == null) {
      return false;
    }
    if (isVoteManager(principal, vote)) {
      return true;
    }
    return voteMembershipRepository.existsByVote_IdAndUser_IdAndStatus(voteId, principal.getId(), "ACTIVE");
  }

  private void validateJoinRequest(VoteInvite invite, VoteJoinRequest request) {
    if (!requiresInvite(invite)) {
      return;
    }
    if (invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new ApiException(HttpStatus.CONFLICT, "邀请码已过期");
    }
    Long voteId = resolveVoteId(invite);
    if (invite.getMaxMembers() != null
        && voteMembershipRepository.countByVote_IdAndStatus(voteId, "ACTIVE") >= invite.getMaxMembers()) {
      throw new ApiException(HttpStatus.CONFLICT, "成员数量已达上限");
    }

    String inviteCode = request == null ? null : request.getInviteCode();
    if (!StringUtils.hasText(inviteCode)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "邀请码不能为空");
    }

    if (!hashInviteCode(inviteCode.trim()).equals(invite.getCodeHash())) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "邀请码无效");
    }
  }

  private VoteInviteDto toDto(VoteInvite invite, boolean canViewPlainCode) {
    Long voteId = resolveVoteId(invite);
    VoteInviteDto dto = new VoteInviteDto();
    dto.setVoteId(voteId);
    dto.setAccessType(requiresInvite(invite) ? "INVITE" : "PUBLIC");
    dto.setEnabled(Boolean.TRUE.equals(invite.getEnabled()));
    dto.setCodeVersion(invite.getCodeVersion());
    dto.setExpiresAt(invite.getExpiresAt());
    dto.setMaxMembers(invite.getMaxMembers());
    dto.setActiveMembers(
        voteMembershipRepository.countByVote_IdAndStatus(voteId, "ACTIVE") > Integer.MAX_VALUE
            ? Integer.MAX_VALUE
            : (int) voteMembershipRepository.countByVote_IdAndStatus(voteId, "ACTIVE"));
    dto.setCanViewPlainCode(canViewPlainCode);

    String plainCode = invite.getCodeCiphertext();
    if (canViewPlainCode) {
      dto.setCode(plainCode);
    }
    dto.setCodeMasked(maskInviteCode(plainCode));
    return dto;
  }

  private VoteInvite createDefaultInvite(Vote vote) {
    String code = generateInviteCode();
    VoteInvite invite = new VoteInvite();
    invite.setVote(vote);
    invite.setEnabled(false);
    invite.setCodeVersion(1);
    invite.setMaxMembers(defaultMaxMembers);
    invite.setCodeHash(hashInviteCode(code));
    invite.setCodeCiphertext(code);
    return invite;
  }

  private Long resolveVoteId(VoteInvite invite) {
    if (invite.getVoteId() != null) {
      return invite.getVoteId();
    }
    return invite.getVote() == null ? null : invite.getVote().getId();
  }

  private Integer resolveMaxMembers(Integer requestedMaxMembers) {
    return requestedMaxMembers == null ? defaultMaxMembers : requestedMaxMembers;
  }

  private LocalDateTime resolveCreateExpiresAt(LocalDateTime requestedExpiresAt, boolean inviteEnabled) {
    if (requestedExpiresAt != null) {
      if (requestedExpiresAt.isBefore(LocalDateTime.now())) {
        throw new ApiException(HttpStatus.BAD_REQUEST, "邀请码过期时间不能早于当前时间");
      }
      return requestedExpiresAt;
    }
    if (!inviteEnabled) {
      return null;
    }
    return LocalDateTime.now().plusHours(defaultExpireHours);
  }

  private VoteJoinResultDto buildJoinResult(Long voteId, String relationship) {
    VoteJoinResultDto result = new VoteJoinResultDto();
    result.setVoteId(voteId);
    result.setRelationship(relationship);
    return result;
  }

  private void requireInviteManager(UserPrincipal principal, Vote vote) {
    if (principal == null) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "未登录");
    }
    if (!isVoteManager(principal, vote)) {
      throw new ApiException(HttpStatus.FORBIDDEN, "无权限管理该投票的邀请码");
    }
  }

  private boolean isVoteManager(UserPrincipal principal, Vote vote) {
    return principal != null
        && (principal.getId().equals(vote.getCreator().getId()) || adminConfirmationService.isAdmin(principal));
  }

  private boolean requiresInvite(VoteInvite invite) {
    return invite != null && Boolean.TRUE.equals(invite.getEnabled());
  }

  private Vote getVote(Long voteId) {
    return voteRepository.findById(voteId)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "投票不存在"));
  }

  private User getUser(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "未登录"));
  }

  private String normalizeAccessType(String accessType) {
    String normalized = accessType.trim().toUpperCase(Locale.ROOT);
    if (!"PUBLIC".equals(normalized) && !"INVITE".equals(normalized)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "访问类型不合法");
    }
    return normalized;
  }

  private String generateInviteCode() {
    StringBuilder builder = new StringBuilder(inviteCodeLength);
    for (int i = 0; i < inviteCodeLength; i++) {
      builder.append(INVITE_CODE_ALPHABET.charAt(secureRandom.nextInt(INVITE_CODE_ALPHABET.length())));
    }
    return builder.toString();
  }

  private String hashInviteCode(String inviteCode) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(inviteCode.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 unavailable", ex);
    }
  }

  private String maskInviteCode(String inviteCode) {
    if (!StringUtils.hasText(inviteCode)) {
      return null;
    }
    if (inviteCode.length() <= 4) {
      return "****";
    }
    return inviteCode.substring(0, 2) + "****" + inviteCode.substring(inviteCode.length() - 2);
  }
}
