package com.vote.backend.service;

import com.vote.backend.controller.ApiException;
import com.vote.backend.entity.User;
import com.vote.backend.entity.Vote;
import com.vote.backend.entity.VoteInvite;
import com.vote.backend.entity.VoteOption;
import com.vote.backend.entity.VoteRecord;
import com.vote.backend.dto.OptionRequest;
import com.vote.backend.dto.VoteCreateRequest;
import com.vote.backend.dto.VoteDto;
import com.vote.backend.dto.VoteDetailDto;
import com.vote.backend.dto.VoteOptionDto;
import com.vote.backend.dto.VoteSubmitRequest;
import com.vote.backend.repository.VoteInviteRepository;
import com.vote.backend.repository.VoteMembershipRepository;
import com.vote.backend.repository.UserRepository;
import com.vote.backend.repository.VoteOptionRepository;
import com.vote.backend.repository.VoteRecordRepository;
import com.vote.backend.repository.VoteRepository;
import com.vote.backend.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

@Service
public class VoteService {

  private static final Logger log = LoggerFactory.getLogger(VoteService.class);

  @Autowired
  private VoteRepository voteRepository;

  @Autowired
  private VoteOptionRepository optionRepository;

  @Autowired
  private VoteRecordRepository recordRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private StringRedisTemplate redisTemplate;

  @Autowired
  private VoteInviteRepository voteInviteRepository;

  @Autowired
  private VoteMembershipRepository voteMembershipRepository;

  @Autowired
  private AdminConfirmationService adminConfirmationService;

  @Transactional
  public boolean castVote(UserPrincipal principal, Long voteId, VoteSubmitRequest request) {
    if (principal == null) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "未登录");
    }
    Vote vote = voteRepository.findById(voteId)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "投票不存在"));
    requireVoteAccess(vote, principal, true);
    User user = userRepository.findById(principal.getId())
        .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "未登录"));
    return castVoteInternal(user, vote, request);
  }

  @Transactional
  public boolean castVote(Long userId, Long voteId, VoteSubmitRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "未登录"));
    Vote vote = voteRepository.findById(voteId)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "投票不存在"));
    return castVoteInternal(user, vote, request);
  }

  private boolean castVoteInternal(User user, Vote vote, VoteSubmitRequest request) {
    Long userId = user.getId();
    Long voteId = vote.getId();
    String userVoteKey = "user:vote:" + userId + ":" + voteId;

    if (!vote.getIsActive() || (vote.getEndTime() != null && LocalDateTime.now().isAfter(vote.getEndTime()))) {
      throw new ApiException(HttpStatus.CONFLICT, "投票已结束");
    }

    List<VoteOption> options = optionRepository.findByVoteIdOrderBySortOrderAsc(voteId);

    long ttlSeconds = TimeUnit.HOURS.toSeconds(24);
    if (vote.getEndTime() != null) {
      long untilEnd = Duration.between(LocalDateTime.now(), vote.getEndTime()).getSeconds();
      if (untilEnd > 0) {
        ttlSeconds = Math.min(ttlSeconds, untilEnd);
      }
    }

    Boolean locked = null;
    try {
      locked = redisTemplate.opsForValue().setIfAbsent(userVoteKey, "1", ttlSeconds, TimeUnit.SECONDS);
    } catch (Exception ex) {
      log.warn("Redis unavailable, skip idempotency key set: {}", ex.getMessage());
    }
    if (Boolean.FALSE.equals(locked)) {
      return false;
    }

    try {
      if (recordRepository.existsByUserIdAndVoteId(userId, voteId)) {
        return false;
      }

      if ("CHOICE".equals(vote.getType())) {
        List<Long> optionIds = request.getOptionIds();
        if (optionIds == null) {
          throw new ApiException(HttpStatus.BAD_REQUEST, "投票选项数量不合法");
        }

        Set<Long> distinctOptionIds = new LinkedHashSet<>(optionIds);
        if (distinctOptionIds.size() != optionIds.size()) {
          throw new ApiException(HttpStatus.BAD_REQUEST, "不能重复选择同一选项");
        }
        if (distinctOptionIds.size() < vote.getMinChoices() || distinctOptionIds.size() > vote.getMaxChoices()) {
          throw new ApiException(HttpStatus.BAD_REQUEST, "投票选项数量不合法");
        }

        for (Long optionId : distinctOptionIds) {
          VoteOption option = options.stream().filter(o -> o.getId().equals(optionId)).findFirst()
              .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "投票选项无效"));

          VoteRecord record = new VoteRecord();
          record.setUser(user);
          record.setVote(vote);
          record.setOption(option);
          recordRepository.save(record);

          try {
            redisTemplate.opsForValue().increment("vote:count:" + voteId + ":" + optionId);
          } catch (Exception ex) {
            log.warn("Redis unavailable, skip count increment: {}", ex.getMessage());
          }
        }
      } else if ("SLIDER".equals(vote.getType())) {
        java.util.Map<Long, Integer> scores = request.getOptionScores();
        if (scores == null || scores.isEmpty()) {
          throw new ApiException(HttpStatus.BAD_REQUEST, "评分数据不能为空");
        }

        if (vote.getForceAllOptions() && scores.size() != options.size()) {
          throw new ApiException(HttpStatus.BAD_REQUEST, "请为所有选项评分");
        }

        boolean hasSavedScore = false;

        for (VoteOption option : options) {
          Integer score = scores.get(option.getId());
          if (score != null) {
            if (score < 0 || score > option.getMaxScore()) {
              throw new ApiException(HttpStatus.BAD_REQUEST, "评分超出范围");
            }

            VoteRecord record = new VoteRecord();
            record.setUser(user);
            record.setVote(vote);
            record.setOption(option);
            record.setScore(score);
            recordRepository.save(record);
            hasSavedScore = true;

            try {
              redisTemplate.opsForValue().increment("vote:count:" + voteId + ":" + option.getId());
              redisTemplate.delete("vote:avg:" + voteId + ":" + option.getId());
            } catch (Exception ex) {
              log.warn("Redis unavailable, skip slider cache update: {}", ex.getMessage());
            }
          }
        }
        if (!hasSavedScore) {
          throw new ApiException(HttpStatus.BAD_REQUEST, "评分数据不能为空");
        }
      } else {
        throw new ApiException(HttpStatus.BAD_REQUEST, "投票类型不合法");
      }

      return true;
    } catch (DataIntegrityViolationException ex) {
      cleanupVoteLock(userVoteKey, locked);
      throw new ApiException(HttpStatus.CONFLICT, "您已投过票或投票无效");
    } catch (Exception ex) {
      cleanupVoteLock(userVoteKey, locked);
      throw ex;
    }
  }

  @Transactional
  public Vote createVote(Long userId, VoteCreateRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "未登录"));

    Vote vote = new Vote();
    vote.setTitle(request.getTitle());
    vote.setDescription(request.getDescription());
    vote.setCreator(user);
    vote.setStartTime(request.getStartTime());
    vote.setEndTime(request.getEndTime());
    vote.setIsActive(true);

    vote.setType(request.getType() != null ? request.getType() : "CHOICE");
    vote.setMinChoices(request.getMinChoices() != null ? request.getMinChoices() : 1);
    vote.setMaxChoices(request.getMaxChoices() != null ? request.getMaxChoices() : 1);
    vote.setForceAllOptions(request.getForceAllOptions() != null ? request.getForceAllOptions() : false);
    vote.setAllowCustomOptions(request.getAllowCustomOptions() != null ? request.getAllowCustomOptions() : false);

    Vote savedVote = voteRepository.save(vote);

    if (request.getOptions() != null) {
      int sortOrder = 1;
      for (OptionRequest optionReq : request.getOptions()) {
        VoteOption option = new VoteOption();
        option.setVote(savedVote);
        option.setOptionText(optionReq.getText());
        option.setMaxScore(optionReq.getMaxScore() != null ? optionReq.getMaxScore() : 100);
        option.setSortOrder(sortOrder++);
        optionRepository.save(option);
      }
    }

    return savedVote;
  }

  public List<VoteDto> getAllVotes() {
    return getAllVotes(null);
  }

  public List<VoteDto> getAllVotes(UserPrincipal principal) {
    Long currentUserId = principal == null ? null : principal.getId();
    boolean isAdmin = adminConfirmationService.isAdmin(principal);
    List<Vote> votes = voteRepository.findAll();
    return votes.stream()
        .filter(vote -> resolveAccessContext(vote, currentUserId, isAdmin).canAccess())
        .map(vote -> {
      VoteDto dto = new VoteDto();
      dto.setId(vote.getId());
      dto.setTitle(vote.getTitle());
      dto.setDescription(vote.getDescription());
      dto.setType(vote.getType());

      LocalDateTime now = LocalDateTime.now();
      if (vote.getEndTime() != null && now.isAfter(vote.getEndTime())) {
        dto.setStatus("closed");
      } else {
        dto.setStatus(vote.getIsActive() ? "active" : "closed");
      }

      dto.setStartTime(vote.getStartTime());
      dto.setEndTime(vote.getEndTime());

      dto.setParticipants(recordRepository.findByVoteId(vote.getId()).stream()
          .map(r -> r.getUser().getId()).collect(Collectors.toSet()).size());

      return dto;
    }).collect(Collectors.toList());
  }

  public VoteDetailDto getVoteDetail(Long voteId, Long currentUserId) {
    return getVoteDetail(voteId, currentUserId, false);
  }

  public VoteDetailDto getVoteDetail(Long voteId, UserPrincipal principal) {
    return getVoteDetail(voteId, principal == null ? null : principal.getId(),
        adminConfirmationService.isAdmin(principal));
  }

  private VoteDetailDto getVoteDetail(Long voteId, Long currentUserId, boolean isAdmin) {
    Vote vote = voteRepository.findById(voteId)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "投票不存在"));
    requireVoteAccess(vote, currentUserId, isAdmin, false);

    VoteDetailDto dto = new VoteDetailDto();
    dto.setId(vote.getId());
    dto.setTitle(vote.getTitle());
    dto.setDescription(vote.getDescription());
    dto.setType(vote.getType());
    dto.setMinChoices(vote.getMinChoices());
    dto.setMaxChoices(vote.getMaxChoices());
    dto.setForceAllOptions(vote.getForceAllOptions());
    dto.setAllowCustomOptions(vote.getAllowCustomOptions());

    LocalDateTime now = LocalDateTime.now();
    if (vote.getEndTime() != null && now.isAfter(vote.getEndTime())) {
      dto.setStatus("closed");
    } else {
      dto.setStatus(vote.getIsActive() ? "active" : "closed");
    }

    dto.setStartTime(vote.getStartTime());
    dto.setEndTime(vote.getEndTime());

    List<VoteOption> options = optionRepository.findByVoteIdOrderBySortOrderAsc(vote.getId());
    List<VoteOptionDto> optionDtos = new ArrayList<>();
    int totalVotes = 0;

    for (VoteOption opt : options) {
      VoteOptionDto optDto = new VoteOptionDto();
      optDto.setId(opt.getId());
      optDto.setText(opt.getOptionText());
      optDto.setMaxScore(opt.getMaxScore());

      if ("CHOICE".equals(vote.getType())) {
        int votes = getOptionVotes(vote.getId(), opt.getId());
        optDto.setVotes(votes);
        totalVotes += votes;
      } else if ("SLIDER".equals(vote.getType())) {
        int votes = getOptionVotes(vote.getId(), opt.getId());
        optDto.setVotes(votes);
        totalVotes += votes;

        Double avg = getOptionAverageScore(vote.getId(), opt.getId());
        optDto.setAverageScore(avg != null ? avg : 0.0);
      }

      optionDtos.add(optDto);
    }

    dto.setOptions(optionDtos);
    dto.setTotalVotes(totalVotes);

    dto.setParticipants(recordRepository.findByVoteId(vote.getId()).stream()
        .map(r -> r.getUser().getId()).collect(Collectors.toSet()).size());

    if (currentUserId != null) {
      dto.setHasVoted(recordRepository.existsByUserIdAndVoteId(currentUserId, voteId));
    } else {
      dto.setHasVoted(false);
    }

    return dto;
  }

  public VoteOptionDto addCustomOption(UserPrincipal principal, Long voteId, OptionRequest request) {
    if (principal == null) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "未登录");
    }
    Vote vote = voteRepository.findById(voteId)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "投票不存在"));
    requireVoteAccess(vote, principal, true);
    return addCustomOption(principal.getId(), voteId, request);
  }

  public VoteOptionDto addCustomOption(Long userId, Long voteId, OptionRequest request) {
    if (request.getText() == null || request.getText().trim().isEmpty()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "选项内容不能为空");
    }
    if (request.getText().length() > 200) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "选项内容不能超过200个字符");
    }

    Vote vote = voteRepository.findById(voteId)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "投票不存在"));
    if (!vote.getIsActive() || (vote.getEndTime() != null && LocalDateTime.now().isAfter(vote.getEndTime()))) {
      throw new ApiException(HttpStatus.CONFLICT, "投票已结束");
    }
    if (!Boolean.TRUE.equals(vote.getAllowCustomOptions())) {
      throw new ApiException(HttpStatus.CONFLICT, "当前投票不允许添加自定义选项");
    }

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "未登录"));

    // 检查是否存在同名选项 (简单防重)
    List<VoteOption> existingOptions = optionRepository.findByVoteIdOrderBySortOrderAsc(voteId);
    boolean exists = existingOptions.stream()
        .anyMatch(opt -> opt.getOptionText().equalsIgnoreCase(request.getText().trim()));
    if (exists) {
      throw new ApiException(HttpStatus.CONFLICT, "该选项已存在");
    }

    // 计算 sortOrder
    int maxSortOrder = existingOptions.stream().mapToInt(VoteOption::getSortOrder).max().orElse(0);

    VoteOption newOption = new VoteOption();
    newOption.setVote(vote);
    newOption.setCreator(user);
    newOption.setOptionText(request.getText().trim());
    newOption.setSortOrder(maxSortOrder + 1);
    newOption.setMaxScore(request.getMaxScore() != null ? request.getMaxScore() : 100);

    VoteOption savedOption = optionRepository.save(newOption);

    VoteOptionDto optDto = new VoteOptionDto();
    optDto.setId(savedOption.getId());
    optDto.setText(savedOption.getOptionText());
    optDto.setMaxScore(savedOption.getMaxScore());
    optDto.setVotes(0);
    optDto.setAverageScore(0.0);

    return optDto;
  }

  private int getOptionVotes(Long voteId, Long optionId) {
    String countKey = "vote:count:" + voteId + ":" + optionId;
    try {
      String countStr = redisTemplate.opsForValue().get(countKey);
      if (countStr != null) {
        return Integer.parseInt(countStr);
      }
    } catch (Exception ex) {
      log.warn("Redis unavailable, fallback to DB count: {}", ex.getMessage());
    }

    int count = recordRepository.countByOptionId(optionId);
    try {
      redisTemplate.opsForValue().set(countKey, String.valueOf(count), 5, TimeUnit.MINUTES);
    } catch (Exception ex) {
      log.warn("Redis unavailable, skip DB count cache set: {}", ex.getMessage());
    }
    return count;
  }

  private Double getOptionAverageScore(Long voteId, Long optionId) {
    String avgKey = "vote:avg:" + voteId + ":" + optionId;
    try {
      String avgStr = redisTemplate.opsForValue().get(avgKey);
      if (avgStr != null) {
        return Double.parseDouble(avgStr);
      }
    } catch (Exception ex) {
      log.warn("Redis unavailable, fallback to DB average: {}", ex.getMessage());
    }

    Optional<Double> avgOpt = recordRepository.findAverageScoreByOptionId(optionId);
    Double avg = avgOpt.orElse(0.0);
    try {
      redisTemplate.opsForValue().set(avgKey, String.valueOf(avg), 5, TimeUnit.MINUTES);
    } catch (Exception ex) {
      log.warn("Redis unavailable, skip DB average cache set: {}", ex.getMessage());
    }
    return avg;
  }

  private void cleanupVoteLock(String userVoteKey, Boolean locked) {
    if (Boolean.TRUE.equals(locked)) {
      try {
        redisTemplate.delete(userVoteKey);
      } catch (Exception redisEx) {
        log.warn("Redis unavailable, skip idempotency key cleanup: {}", redisEx.getMessage());
      }
    }
  }

  private void requireVoteAccess(Vote vote, UserPrincipal principal, boolean writeAccess) {
    requireVoteAccess(vote, principal == null ? null : principal.getId(),
        adminConfirmationService.isAdmin(principal), writeAccess);
  }

  private void requireVoteAccess(Vote vote, Long userId, boolean isAdmin, boolean writeAccess) {
    VoteAccessContext accessContext = resolveAccessContext(vote, userId, isAdmin);
    if (accessContext.canAccess()) {
      return;
    }
    if (writeAccess && userId == null) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "未登录");
    }
    if (accessContext.inviteOnly()) {
      throw new ApiException(HttpStatus.FORBIDDEN, "无权访问该投票");
    }
    throw new ApiException(HttpStatus.FORBIDDEN, "无权操作该投票");
  }

  private VoteAccessContext resolveAccessContext(Vote vote, Long userId, boolean isAdmin) {
    VoteInvite invite = voteInviteRepository.findById(vote.getId()).orElse(null);
    boolean inviteOnly = requiresInvite(invite);
    String relationship = resolveRelationship(vote, userId, isAdmin, inviteOnly);
    boolean canAccess = !inviteOnly
        || "CREATOR".equals(relationship)
        || "ADMIN".equals(relationship)
        || "MEMBER".equals(relationship);
    return new VoteAccessContext(inviteOnly, relationship, canAccess);
  }

  private String resolveRelationship(Vote vote, Long userId, boolean isAdmin, boolean inviteOnly) {
    if (userId == null) {
      return "ANONYMOUS";
    }
    if (vote.getCreator() != null && userId.equals(vote.getCreator().getId())) {
      return "CREATOR";
    }
    if (isAdmin) {
      return "ADMIN";
    }
    if (inviteOnly && voteMembershipRepository.existsByVote_IdAndUser_IdAndStatus(vote.getId(), userId, "ACTIVE")) {
      return "MEMBER";
    }
    return "VISITOR";
  }

  private boolean requiresInvite(VoteInvite invite) {
    return invite != null && Boolean.TRUE.equals(invite.getEnabled());
  }

  private record VoteAccessContext(boolean inviteOnly, String relationship, boolean canAccess) {
  }
}
