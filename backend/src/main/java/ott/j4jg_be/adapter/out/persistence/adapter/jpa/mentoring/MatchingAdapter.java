package ott.j4jg_be.adapter.out.persistence.adapter.jpa.mentoring;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ott.j4jg_be.adapter.out.persistence.entity.jpa.mentoring.MatchingEntity;
import ott.j4jg_be.adapter.out.persistence.repository.jpa.mentoring.MatchingRepository;
import ott.j4jg_be.application.port.out.mentoring.GetMatchingPort;
import ott.j4jg_be.application.port.out.mentoring.MatchingPort;

@Component
@RequiredArgsConstructor
public class MatchingAdapter implements MatchingPort, GetMatchingPort {

    private final MatchingRepository matchingRepository;

    @Override
    public int matching(String userId, int mentoringId) {

        MatchingEntity matchingEntity = matchingRepository.save(new MatchingEntity(userId, mentoringId));

        return matchingEntity.getMatchingId();
    }

    @Override
    public boolean existsMatching(int mentoringId) {
        return matchingRepository.existsByMentoringId(mentoringId);
    }
}
