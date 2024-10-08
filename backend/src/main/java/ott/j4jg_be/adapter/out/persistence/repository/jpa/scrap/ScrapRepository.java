package ott.j4jg_be.adapter.out.persistence.repository.jpa.scrap;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ott.j4jg_be.adapter.out.persistence.entity.jpa.scrap.ScrapEntity;

public interface ScrapRepository extends JpaRepository<ScrapEntity, Integer> {

    ScrapEntity findByUserIdAndJobInfoId(String userId, int jobInfoId);

    Page<ScrapEntity> findByUserIdAndStatus(String userId, boolean b, Pageable pageable);
}
