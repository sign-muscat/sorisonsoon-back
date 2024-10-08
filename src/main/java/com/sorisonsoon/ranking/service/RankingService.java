package com.sorisonsoon.ranking.service;

import com.sorisonsoon.common.domain.type.GameCategory;
import com.sorisonsoon.ranking.domain.entity.Ranking;
import com.sorisonsoon.ranking.domain.repository.RankingRepository;
import com.sorisonsoon.ranking.dto.RankingDTO;
import com.sorisonsoon.ranking.dto.response.RankResponse;
import com.sorisonsoon.user.dto.response.NickNameUserInfo;
import com.sorisonsoon.user.service.UserService;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class RankingService {
    private final RankingRepository rankingRepository;
    private final UserService userService;

    public void save(Long userId, GameCategory category, double score) {
        final Ranking newRanking = Ranking.of(
                userId,
                category,
                (int) Math.round(score * 100)
        );
        rankingRepository.save(newRanking);
    }

    @Transactional(readOnly = true)
    public List<RankingDTO> getTop10ByCategory(GameCategory category) {
        Pageable topTen = PageRequest.of(0, 10);
        List<Ranking> rankings = rankingRepository.findTop10ByCategory(category, topTen);
        return rankings.stream()
                .map(ranking -> new RankingDTO(ranking.getUserId(), ranking.getScore()))
                .collect(Collectors.toList());
    }

    public Map<GameCategory, List<RankResponse>> getTodayRanks(int limit) {
        Map<GameCategory, List<RankResponse>> rankingsByCategory = rankingRepository.getTodayRanks(limit);

        Map<GameCategory, List<RankResponse>> enrichedRankings = new HashMap<>();

        for (Map.Entry<GameCategory, List<RankResponse>> entry : rankingsByCategory.entrySet()) {
            List<RankResponse> enrichedRankResponses = new ArrayList<>();
            for (RankResponse rankResponse : entry.getValue()) {
                NickNameUserInfo userInfo = userService.getUserNickname(rankResponse.getUserId());
                String nickname = userInfo.getNickname();

                RankResponse enrichedRankResponse = new RankResponse(
                        rankResponse.getRankingId(),
                        rankResponse.getUserId(),
                        rankResponse.getCategory(),
                        rankResponse.getScore(),
                        rankResponse.getCreatedAt(),
                        nickname
                );
                enrichedRankResponses.add(enrichedRankResponse);
            }
            enrichedRankings.put(entry.getKey(), enrichedRankResponses);
        }

        return enrichedRankings;
    }

    public List<RankResponse> getMyRanks(Long userId) {
        List<RankResponse> myRankings = rankingRepository.getMyRanks(userId);
        return myRankings;
    }
}