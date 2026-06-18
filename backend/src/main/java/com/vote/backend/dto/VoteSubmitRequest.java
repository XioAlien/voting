package com.vote.backend.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class VoteSubmitRequest {
    private List<Long> optionIds; // For CHOICE mode
    private Map<Long, Integer> optionScores; // For SLIDER mode
}
