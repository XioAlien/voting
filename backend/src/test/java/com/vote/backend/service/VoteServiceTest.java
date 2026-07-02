package com.vote.backend.service;

import com.vote.backend.controller.ApiException;
import com.vote.backend.dto.OptionRequest;
import com.vote.backend.dto.VoteCreateRequest;
import com.vote.backend.dto.VoteDetailDto;
import com.vote.backend.dto.VoteDto;
import com.vote.backend.dto.VoteSubmitRequest;
import com.vote.backend.entity.User;
import com.vote.backend.entity.Vote;
import com.vote.backend.entity.VoteInvite;
import com.vote.backend.entity.VoteOption;
import com.vote.backend.entity.VoteRecord;
import com.vote.backend.repository.VoteInviteRepository;
import com.vote.backend.repository.VoteMembershipRepository;
import com.vote.backend.repository.UserRepository;
import com.vote.backend.repository.VoteOptionRepository;
import com.vote.backend.repository.VoteRecordRepository;
import com.vote.backend.repository.VoteRepository;
import com.vote.backend.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VoteServiceTest {

  @Mock
  private VoteRepository voteRepository;

  @Mock
  private VoteOptionRepository optionRepository;

  @Mock
  private VoteRecordRepository recordRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private StringRedisTemplate redisTemplate;

  @Mock
  private VoteInviteRepository voteInviteRepository;

  @Mock
  private VoteMembershipRepository voteMembershipRepository;

  @Mock
  private AdminConfirmationService adminConfirmationService;

  @Mock
  private VoteInviteService voteInviteService;

  @Mock
  private ValueOperations<String, String> valueOperations;

  @InjectMocks
  private VoteService voteService;

  private User testUser;
  private Vote testVote;
  private VoteOption testOption;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setId(1L);
    testUser.setUsername("testuser");

    testVote = new Vote();
    testVote.setId(1L);
    testVote.setTitle("Test Vote");
    testVote.setDescription("Test Description");
    testVote.setCreator(testUser);
    testVote.setIsActive(true);
    testVote.setType("CHOICE");
    testVote.setMinChoices(1);
    testVote.setMaxChoices(1);
    testVote.setAllowCustomOptions(true);

    testOption = new VoteOption();
    testOption.setId(1L);
    testOption.setVote(testVote);
    testOption.setOptionText("Option 1");
    testOption.setMaxScore(100);
  }

  @Test
  void createVote_ShouldCreateVoteAndOptions() {
    VoteCreateRequest request = new VoteCreateRequest();
    request.setTitle("New Vote");
    request.setType("CHOICE");

    OptionRequest opt1 = new OptionRequest();
    opt1.setText("A");
    OptionRequest opt2 = new OptionRequest();
    opt2.setText("B");
    request.setOptions(Arrays.asList(opt1, opt2));

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(voteRepository.save(any(Vote.class))).thenReturn(testVote);

    Vote createdVote = voteService.createVote(1L, request);

    assertNotNull(createdVote);
    assertEquals("Test Vote", createdVote.getTitle());
    verify(voteRepository, times(1)).save(any(Vote.class));
    verify(optionRepository, times(2)).save(any(VoteOption.class));
  }

  @Test
  void getAllVotes_ShouldReturnListOfVotes() {
    when(voteRepository.findAll()).thenReturn(Arrays.asList(testVote));
    when(recordRepository.findByVoteId(1L)).thenReturn(Arrays.asList());
    when(adminConfirmationService.isAdmin(null)).thenReturn(false);
    when(voteInviteRepository.findById(1L)).thenReturn(Optional.empty());

    List<VoteDto> votes = voteService.getAllVotes();

    assertEquals(1, votes.size());
    assertEquals("Test Vote", votes.get(0).getTitle());
    assertEquals(0, votes.get(0).getParticipants());
  }

  @Test
  void getVoteDetail_ShouldReturnVoteDetails() {
    when(voteRepository.findById(1L)).thenReturn(Optional.of(testVote));
    when(optionRepository.findByVoteIdOrderBySortOrderAsc(1L)).thenReturn(Arrays.asList(testOption));
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get(anyString())).thenReturn("10"); // 10 votes for option
    when(recordRepository.existsByUserIdAndVoteId(1L, 1L)).thenReturn(true); // User has voted
    when(voteInviteRepository.findById(1L)).thenReturn(Optional.empty());

    VoteDetailDto detail = voteService.getVoteDetail(1L, 1L);

    assertNotNull(detail);
    assertEquals("Test Vote", detail.getTitle());
    assertEquals(1, detail.getOptions().size());
    assertEquals(10, detail.getTotalVotes());
    assertTrue(detail.isHasVoted());
  }

  @Test
  void castVote_Slider_ShouldCalculateScoresCorrectly() {
    testVote.setType("SLIDER");
    testVote.setForceAllOptions(true);

    VoteOption opt2 = new VoteOption();
    opt2.setId(2L);
    opt2.setMaxScore(100);
    opt2.setVote(testVote);

    VoteSubmitRequest req = new VoteSubmitRequest();
    Map<Long, Integer> scores = new HashMap<>();
    scores.put(1L, 85);
    scores.put(2L, 90);
    req.setOptionScores(scores);

    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
    when(recordRepository.existsByUserIdAndVoteId(1L, 1L)).thenReturn(false);
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(voteRepository.findById(1L)).thenReturn(Optional.of(testVote));
    when(optionRepository.findByVoteIdOrderBySortOrderAsc(1L)).thenReturn(Arrays.asList(testOption, opt2));

    boolean result = voteService.castVote(1L, 1L, req);

    assertTrue(result);
    verify(recordRepository, times(2)).save(any(VoteRecord.class));
    verify(valueOperations, times(2)).increment(anyString());
    verify(redisTemplate, times(2)).delete(anyString());
  }

  @Test
  void castVote_Choice_ShouldAllowMultipleDistinctOptionsInOneVote() {
    testVote.setType("CHOICE");
    testVote.setMinChoices(1);
    testVote.setMaxChoices(2);

    VoteOption opt2 = new VoteOption();
    opt2.setId(2L);
    opt2.setVote(testVote);
    opt2.setOptionText("Option 2");

    VoteSubmitRequest req = new VoteSubmitRequest();
    req.setOptionIds(Arrays.asList(1L, 2L));

    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
    when(recordRepository.existsByUserIdAndVoteId(1L, 1L)).thenReturn(false);
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(voteRepository.findById(1L)).thenReturn(Optional.of(testVote));
    when(optionRepository.findByVoteIdOrderBySortOrderAsc(1L)).thenReturn(Arrays.asList(testOption, opt2));

    boolean result = voteService.castVote(1L, 1L, req);

    assertTrue(result);
    verify(recordRepository, times(2)).save(any(VoteRecord.class));
    verify(valueOperations, times(2)).increment(anyString());
  }

  @Test
  void castVote_Choice_ShouldRejectDuplicateOptionIds() {
    testVote.setType("CHOICE");
    testVote.setMinChoices(1);
    testVote.setMaxChoices(2);

    VoteSubmitRequest req = new VoteSubmitRequest();
    req.setOptionIds(Arrays.asList(1L, 1L));

    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
    when(recordRepository.existsByUserIdAndVoteId(1L, 1L)).thenReturn(false);
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(voteRepository.findById(1L)).thenReturn(Optional.of(testVote));
    when(optionRepository.findByVoteIdOrderBySortOrderAsc(1L)).thenReturn(Arrays.asList(testOption));

    ApiException ex = assertThrows(ApiException.class, () -> voteService.castVote(1L, 1L, req));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    assertEquals("不能重复选择同一选项", ex.getMessage());
    verify(recordRepository, never()).save(any(VoteRecord.class));
    verify(redisTemplate).delete("user:vote:1:1");
  }

  @Test
  void castVote_ShouldTranslateDatabaseUniqueConstraintViolation() {
    VoteSubmitRequest req = new VoteSubmitRequest();
    req.setOptionIds(Arrays.asList(1L));

    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
    when(recordRepository.existsByUserIdAndVoteId(1L, 1L)).thenReturn(false);
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(voteRepository.findById(1L)).thenReturn(Optional.of(testVote));
    when(optionRepository.findByVoteIdOrderBySortOrderAsc(1L)).thenReturn(Arrays.asList(testOption));
    when(recordRepository.save(any(VoteRecord.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

    ApiException ex = assertThrows(ApiException.class, () -> voteService.castVote(1L, 1L, req));

    assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    assertEquals("您已投过票或投票无效", ex.getMessage());
    verify(redisTemplate).delete("user:vote:1:1");
  }

  @Test
  void addCustomOption_ShouldAddOption() {
    OptionRequest request = new OptionRequest();
    request.setText("New Custom Option");
    request.setMaxScore(100);

    when(voteRepository.findById(1L)).thenReturn(Optional.of(testVote));
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(optionRepository.findByVoteIdOrderBySortOrderAsc(1L)).thenReturn(Arrays.asList(testOption));

    VoteOption newOption = new VoteOption();
    newOption.setId(2L);
    newOption.setOptionText("New Custom Option");
    newOption.setMaxScore(100);
    when(optionRepository.save(any(VoteOption.class))).thenReturn(newOption);

    com.vote.backend.dto.VoteOptionDto result = voteService.addCustomOption(1L, 1L, request);

    assertNotNull(result);
    assertEquals("New Custom Option", result.getText());
    assertEquals(100, result.getMaxScore());
    verify(optionRepository, times(1)).save(any(VoteOption.class));
  }

  @Test
  void getAllVotes_ShouldHideInviteVoteFromAnonymousUser() {
    VoteInvite invite = new VoteInvite();
    invite.setVoteId(1L);
    invite.setEnabled(true);

    when(voteRepository.findAll()).thenReturn(Arrays.asList(testVote));
    when(adminConfirmationService.isAdmin(null)).thenReturn(false);
    when(voteInviteRepository.findById(1L)).thenReturn(Optional.of(invite));

    List<VoteDto> votes = voteService.getAllVotes();

    assertTrue(votes.isEmpty());
    verify(recordRepository, never()).findByVoteId(anyLong());
  }

  @Test
  void getVoteDetail_ShouldRejectInviteVoteForNonMember() {
    VoteInvite invite = new VoteInvite();
    invite.setVoteId(1L);
    invite.setEnabled(true);

    when(voteRepository.findById(1L)).thenReturn(Optional.of(testVote));
    when(voteInviteRepository.findById(1L)).thenReturn(Optional.of(invite));
    when(voteMembershipRepository.existsByVote_IdAndUser_IdAndStatus(1L, 2L, "ACTIVE")).thenReturn(false);

    ApiException ex = assertThrows(ApiException.class, () -> voteService.getVoteDetail(1L, 2L));

    assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    assertEquals("无权访问该投票", ex.getMessage());
  }

  @Test
  void getVoteDetail_ShouldAllowInviteVoteForActiveMember() {
    VoteInvite invite = new VoteInvite();
    invite.setVoteId(1L);
    invite.setEnabled(true);

    UserPrincipal principal = userPrincipal(2L, "USER");

    when(voteRepository.findById(1L)).thenReturn(Optional.of(testVote));
    when(voteInviteRepository.findById(1L)).thenReturn(Optional.of(invite));
    when(voteMembershipRepository.existsByVote_IdAndUser_IdAndStatus(1L, 2L, "ACTIVE")).thenReturn(true);
    when(adminConfirmationService.isAdmin(principal)).thenReturn(false);
    when(optionRepository.findByVoteIdOrderBySortOrderAsc(1L)).thenReturn(Arrays.asList(testOption));
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get(anyString())).thenReturn("0");
    when(recordRepository.findByVoteId(1L)).thenReturn(Arrays.asList());
    when(recordRepository.existsByUserIdAndVoteId(2L, 1L)).thenReturn(false);

    VoteDetailDto detail = voteService.getVoteDetail(1L, principal);

    assertNotNull(detail);
    assertEquals(1L, detail.getId());
  }

  @Test
  void castVote_ShouldRejectInviteVoteForNonMemberBeforeEnteringVoteFlow() {
    VoteInvite invite = new VoteInvite();
    invite.setVoteId(1L);
    invite.setEnabled(true);

    UserPrincipal principal = userPrincipal(2L, "USER");
    VoteSubmitRequest req = new VoteSubmitRequest();
    req.setOptionIds(Arrays.asList(1L));

    when(voteRepository.findById(1L)).thenReturn(Optional.of(testVote));
    when(voteInviteRepository.findById(1L)).thenReturn(Optional.of(invite));
    when(voteMembershipRepository.existsByVote_IdAndUser_IdAndStatus(1L, 2L, "ACTIVE")).thenReturn(false);
    when(adminConfirmationService.isAdmin(principal)).thenReturn(false);

    ApiException ex = assertThrows(ApiException.class, () -> voteService.castVote(principal, 1L, req));

    assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    assertEquals("无权访问该投票", ex.getMessage());
    verify(redisTemplate, never()).opsForValue();
    verify(recordRepository, never()).save(any(VoteRecord.class));
  }

  private UserPrincipal userPrincipal(Long userId, String role) {
    return new UserPrincipal(userId, "user" + userId, "pwd",
        List.of(new SimpleGrantedAuthority("ROLE_" + role)));
  }
}
