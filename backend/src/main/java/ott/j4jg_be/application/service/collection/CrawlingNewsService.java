package ott.j4jg_be.application.service.collection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ott.j4jg_be.adapter.out.crawler.dto.NewsDTO;
import ott.j4jg_be.application.port.in.collection.CrawlingNewsUsecase;
import ott.j4jg_be.application.port.out.collection.CrawlingNewsPort;
import ott.j4jg_be.application.port.out.collection.JobInfoPort;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrawlingNewsService implements CrawlingNewsUsecase {

    private final CrawlingNewsPort crawlingNewsPort;
    private final JobInfoPort jobInfoPort;
    private final ObjectMapper objectMapper;

    @Override
    public void CrawlingNews() {
        int page = 0;
        boolean hasMoreData;

        do {
            List<String> companyNames = jobInfoPort.getCompanyNames(page, 100);
            hasMoreData = !companyNames.isEmpty();

            if (hasMoreData) {
                processNews(companyNames);
                page++;
            }
        } while (hasMoreData);
    }

    private void processNews(List<String> companyNames) {
        List<NewsDTO> newsList = crawlingNewsPort.crawlingNews(companyNames);

        newsList.forEach(this::logNews);
    }

    private void logNews(NewsDTO news) {
        try {
            log.info(objectMapper.writeValueAsString(news));
        } catch (JsonProcessingException e) {
            log.warn("Error processing JSON", e);
        }
    }
}
