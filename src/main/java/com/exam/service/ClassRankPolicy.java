package com.exam.service;

import com.exam.model.ClassRank;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ClassRankPolicy {

    private final List<RankThreshold> thresholds = List.of(
            new RankThreshold(500, ClassRank.A),
            new RankThreshold(300, ClassRank.B),
            new RankThreshold(120, ClassRank.C),
            new RankThreshold(0, ClassRank.D)
    );

    public ClassRank resolve(int classPoints) {
        return thresholds.stream()
                .filter(threshold -> classPoints >= threshold.minPoints())
                .findFirst()
                .map(RankThreshold::rank)
                .orElse(ClassRank.D);
    }

    private record RankThreshold(int minPoints, ClassRank rank) {
    }
}
