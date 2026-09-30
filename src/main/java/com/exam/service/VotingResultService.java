package com.exam.service;

import com.exam.exception.BadRequestException;
import com.exam.model.SecretVoting;
import com.exam.model.Vote;
import com.exam.model.VotingOption;
import com.exam.model.VotingResult;
import com.exam.model.VotingStatus;
import com.exam.repository.VoteRepository;
import com.exam.repository.VotingOptionRepository;
import com.exam.repository.VotingResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class VotingResultService {

    private final VotingOptionRepository optionRepository;
    private final VoteRepository voteRepository;
    private final VotingResultRepository resultRepository;

    public VotingResultService(
            VotingOptionRepository optionRepository,
            VoteRepository voteRepository,
            VotingResultRepository resultRepository
    ) {
        this.optionRepository = optionRepository;
        this.voteRepository = voteRepository;
        this.resultRepository = resultRepository;
    }

    @Transactional
    public void calculateAndSaveResults(SecretVoting voting) {
        if (voting.getStatus() != VotingStatus.FINISHED) {
            throw new IllegalStateException("Final results can only be calculated for a finished voting");
        }
        List<VotingOption> options = optionRepository.findByVoting_Id(voting.getId());
        Map<Long, Long> counts = new LinkedHashMap<>();
        options.forEach(option -> counts.put(option.getId(), 0L));
        for (Vote vote : voteRepository.findByVoting_Id(voting.getId())) {
            Long optionId = decodeVote(vote.getEncryptedValue());
            if (counts.containsKey(optionId)) {
                counts.computeIfPresent(optionId, (ignored, count) -> count + 1);
            }
        }
        for (VotingOption option : options) {
            VotingResult result = resultRepository.findByVotingIdAndOptionId(voting.getId(), option.getId())
                    .orElseGet(VotingResult::new);
            result.setVoting(voting);
            result.setOption(option);
            result.setVotesCount(counts.get(option.getId()));
            result.setCalculatedAt(com.exam.util.DateTimeUtils.nowUtc());
            resultRepository.save(result);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getStoredResults(SecretVoting voting) {
        if (voting.getStatus() != VotingStatus.FINISHED) {
            throw new BadRequestException("Final results are available after voting is finished");
        }
        Map<String, Long> results = new LinkedHashMap<>();
        resultRepository.findByVotingIdOrderByOptionId(voting.getId())
                .forEach(result -> results.put(result.getOption().getLabel(), result.getVotesCount()));
        return results;
    }

    private Long decodeVote(String encryptedValue) {
        String decoded = new String(Base64.getDecoder().decode(encryptedValue), StandardCharsets.UTF_8);
        return Long.valueOf(decoded);
    }
}
