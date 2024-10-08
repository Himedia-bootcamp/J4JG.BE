package ott.j4jg_be.adapter.out.persistence.mapper.scrap;

import org.springframework.stereotype.Component;
import ott.j4jg_be.adapter.out.persistence.entity.jpa.scrap.ScrapEntity;
import ott.j4jg_be.domain.scrap.Scrap;

@Component
public class ScrapEntityMapper {

    public ScrapEntity mapToEntity(Scrap scrap){

        return new ScrapEntity(
                scrap.getUserId(),
                scrap.getJobInfoId(),
                scrap.isStatus()
        );
    }

    public Scrap mapToDomain(ScrapEntity scrapEntity){
        return new Scrap(
                scrapEntity.getScrapId(),
                scrapEntity.getUserId(),
                scrapEntity.getJobInfoId(),
                scrapEntity.getStatus()
        );
    }
}
