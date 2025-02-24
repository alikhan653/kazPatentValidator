package kz.it.patentparser.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.it.patentparser.dto.PatentDto;
import kz.it.patentparser.parser.EbulletinPatentParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
public class PatentApiClient {
    private static final Logger logger = LoggerFactory.getLogger(EbulletinPatentParser.class);
    private static final String BASE_URL = "https://ebulletin.kazpatent.kz:6002";
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public PatentApiClient(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.baseUrl(BASE_URL).build();
        this.objectMapper = objectMapper;
    }

    public Flux<PatentDto> fetchPatents(String endpoint, int page, String date) {
        // Log request
        logger.info("Fetching patents from endpoint: {}/bulletin/published/{}/{}/{}", BASE_URL, endpoint, page, date);

        return webClient.get()
                .uri("/bulletin/published/{endpoint}/{page}/{date}", endpoint, page, date)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::decodeUnicode)
                .flatMapMany(Flux::fromIterable);
    }



    private List<PatentDto> decodeUnicode(String json) {
        try {
//            String decodedJson = objectMapper.readTree(json).toString(); // Decodes Unicode
            return objectMapper.readValue(json, new TypeReference<List<PatentDto>>() {});
        } catch (JsonProcessingException e) {
            logger.error("Error decoding Unicode JSON response: {}", json, e);
            throw new RuntimeException("Error decoding Unicode JSON response", e);
        }
    }

}
