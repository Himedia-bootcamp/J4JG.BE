package ott.j4jg_be.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;
import ott.j4jg_be.adapter.out.persistence.entity.jpa.point.PointHistoryEntity;
import ott.j4jg_be.domain.point.PointHistory;

@Component
public class PointHistoryEntityMapper {

  public PointHistoryEntity toEntity(String userId, PointHistory pointHistory) {
    return PointHistoryEntity.builder()
        .userId(userId)
        .points(pointHistory.getPoints())
        .reason(pointHistory.getReason())
        .createdAt(pointHistory.getCreatedAt())
        .finalBalance(pointHistory.getFinalBalance())
        .build();
  }
}
