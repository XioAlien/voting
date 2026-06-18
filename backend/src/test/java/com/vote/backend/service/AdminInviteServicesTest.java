package com.vote.backend.service;

import com.vote.backend.controller.ApiException;
import com.vote.backend.dto.AdminConfirmRequest;
import com.vote.backend.dto.AdminDashboardDto;
import com.vote.backend.dto.AdminUserStatusUpdateRequest;
import com.vote.backend.dto.VoteInviteDto;
import com.vote.backend.dto.VoteJoinRequest;
import com.vote.backend.dto.VoteJoinResultDto;
import com.vote.backend.entity.User;
import com.vote.backend.entity.Vote;
import com.vote.backend.entity.VoteInvite;
import com.vote.backend.repository.AdminAuditLogRepository;
import com.vote.backend.repository.UserRepository;
import com.vote.backend.repository.VoteInviteRepository;
import com.vote.backend.repository.VoteMembershipRepository;
import com.vote.backend.repository.VoteRecordRepository;
import com.vote.backend.repository.VoteRepository;
import com.vote.backend.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminInviteServicesTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private VoteRepository voteRepository;

  @Mock
  private VoteRecordRepository voteRecordRepository;

  @Mock
  private AdminAuditLogRepository adminAuditLogRepository;

  @Mock
  private VoteInviteRepository voteInviteRepository;

  @Mock
  private VoteMembershipRepository voteMembershipRepository;

  @Mock
  private AdminAuditService adminAuditService;

  private final AdminConfirmationService adminConfirmationService = new AdminConfirmationService();

  private AdminService adminService;

  private VoteInviteService voteInviteService;

  @BeforeEach
  void setUp() {
    adminService = new AdminService(
        userRepository,
        voteRepository,
        voteRecordRepository,
        adminAuditLogRepository,
        adminAuditService,
        adminConfirmationService);
    voteInviteService = new VoteInviteService(
        voteRepository,
        voteInviteRepository,
        voteMembershipRepository,
        userRepository,
        adminAuditService,
        adminConfirmationService,
        100,
        8);
  }

  @Test
  void issueAndValidateToken_ShouldSucceedForSameAdminAndTarget() {
    UserPrincipal admin = adminPrincipal(99L);
    AdminConfirmRequest request = new AdminConfirmRequest();
    request.setAction("update_user_role");
    request.setTarget("1");

    String token = adminConfirmationService.issueToken(admin, request);

    adminConfirmationService.validateToken(admin, token, "UPDATE_USER_ROLE", "1");
  }

  @Test
  void getDashboard_ShouldUseCurrentUserEntitySemantics() {
    Vote activeVote = new Vote();
    activeVote.setIsActive(true);
    activeVote.setEndTime(LocalDateTime.now().plusDays(1));

    Vote closedVote = new Vote();
    closedVote.setIsActive(false);
    closedVote.setEndTime(LocalDateTime.now().minusDays(1));

    when(userRepository.count()).thenReturn(5L);
    when(voteRepository.count()).thenReturn(2L);
    when(voteRepository.findAll()).thenReturn(List.of(activeVote, closedVote));
    when(voteRecordRepository.count()).thenReturn(12L);
    when(adminAuditLogRepository.count()).thenReturn(3L);

    AdminDashboardDto result = adminService.getDashboard(adminPrincipal(1L));

    assertEquals(5L, result.getTotalUsers());
    assertEquals(5L, result.getActiveUsers());
    assertEquals(2L, result.getTotalVotes());
    assertEquals(1L, result.getActiveVotes());
    assertEquals(12L, result.getTotalVoteRecords());
    assertEquals(3L, result.getTotalAuditLogs());
  }

  @Test
  void updateUserStatus_ShouldRejectDisableWhenEntityHasNoDisabledField() {
    AdminUserStatusUpdateRequest request = new AdminUserStatusUpdateRequest();
    request.setDisabled(true);

    String token = issueToken("UPDATE_USER_STATUS", "2");
    request.setConfirmToken(token);

    ApiException ex = assertThrows(ApiException.class,
        () -> adminService.updateUserStatus(adminPrincipal(1L), 2L, request));

    assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    assertEquals("当前用户实体未提供禁用状态，暂不支持禁用账号", ex.getMessage());
    verify(adminAuditService, never()).record(any(), any(), any(), any(), any(), any(Boolean.class));
  }

  @Test
  void joinVote_ShouldActivateMembershipWhenInviteMatches() {
    User creator = new User();
    creator.setId(10L);

    Vote vote = new Vote();
    vote.setId(1L);
    vote.setCreator(creator);

    User member = new User();
    member.setId(2L);

    VoteInvite invite = new VoteInvite();
    invite.setVoteId(1L);
    invite.setVote(vote);
    invite.setEnabled(true);
    invite.setCodeVersion(3);
    invite.setMaxMembers(100);
    invite.setCodeCiphertext("ABCD2345");
    invite.setCodeHash(hash("ABCD2345"));

    when(voteRepository.findById(1L)).thenReturn(Optional.of(vote));
    when(userRepository.findById(2L)).thenReturn(Optional.of(member));
    when(voteInviteRepository.findById(1L)).thenReturn(Optional.of(invite));
    when(voteMembershipRepository.findByVote_IdAndUser_Id(1L, 2L)).thenReturn(Optional.empty());
    when(voteMembershipRepository.countByVote_IdAndStatus(1L, "ACTIVE")).thenReturn(0L);

    VoteJoinRequest request = new VoteJoinRequest();
    request.setInviteCode("ABCD2345");

    VoteJoinResultDto result = voteInviteService.joinVote(userPrincipal(2L, "USER"), 1L, request);

    assertEquals(1L, result.getVoteId());
    assertEquals("MEMBER", result.getRelationship());

    ArgumentCaptor<com.vote.backend.entity.VoteMembership> captor = ArgumentCaptor
        .forClass(com.vote.backend.entity.VoteMembership.class);
    verify(voteMembershipRepository).save(captor.capture());
    assertEquals("ACTIVE", captor.getValue().getStatus());
    assertEquals(3, captor.getValue().getJoinedCodeVersion());
  }

  @Test
  void getInvite_ShouldUseConfiguredDefaultsWhenInviteMissing() {
    User creator = new User();
    creator.setId(1L);

    Vote vote = new Vote();
    vote.setId(7L);
    vote.setCreator(creator);

    when(voteRepository.findById(7L)).thenReturn(Optional.of(vote));
    when(voteInviteRepository.findById(7L)).thenReturn(Optional.empty());
    when(voteMembershipRepository.countByVote_IdAndStatus(7L, "ACTIVE")).thenReturn(0L);

    VoteInviteDto result = voteInviteService.getInvite(adminPrincipal(1L), 7L);

    assertEquals("PUBLIC", result.getAccessType());
    assertEquals(100, result.getMaxMembers());
    assertEquals(1, result.getCodeVersion());
    assertEquals(0, result.getActiveMembers());
    assertTrue(Boolean.TRUE.equals(result.getCanViewPlainCode()));
  }

  @Test
  void resetInviteCode_ShouldRespectConfiguredInviteDefaults() {
    VoteInviteService configuredService = new VoteInviteService(
        voteRepository,
        voteInviteRepository,
        voteMembershipRepository,
        userRepository,
        adminAuditService,
        adminConfirmationService,
        50,
        10);

    User creator = new User();
    creator.setId(1L);

    Vote vote = new Vote();
    vote.setId(8L);
    vote.setCreator(creator);

    User admin = new User();
    admin.setId(1L);

    when(voteRepository.findById(8L)).thenReturn(Optional.of(vote));
    when(voteInviteRepository.findById(8L)).thenReturn(Optional.empty());
    when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
    when(voteInviteRepository.save(any(VoteInvite.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(voteMembershipRepository.countByVote_IdAndStatus(8L, "ACTIVE")).thenReturn(0L);

    VoteInviteDto result = configuredService.resetInviteCode(adminPrincipal(1L), 8L);

    assertEquals(50, result.getMaxMembers());
    assertEquals(10, result.getCode().length());
    assertEquals("PUBLIC", result.getAccessType());
  }

  private String issueToken(String action, String target) {
    AdminConfirmRequest request = new AdminConfirmRequest();
    request.setAction(action);
    request.setTarget(target);
    return adminConfirmationService.issueToken(adminPrincipal(1L), request);
  }

  private UserPrincipal adminPrincipal(Long userId) {
    return userPrincipal(userId, "ADMIN");
  }

  private UserPrincipal userPrincipal(Long userId, String role) {
    return new UserPrincipal(userId, "user" + userId, "pwd",
        List.of(new SimpleGrantedAuthority("ROLE_" + role)));
  }

  private String hash(String raw) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }
}
