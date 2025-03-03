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
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

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
        logger.info("Fetching patents from endpoint: {}/bulletin/published/{}/{}/{}", BASE_URL, endpoint, page, date);

        return webClient.get()
                .uri("/bulletin/published/{endpoint}/{page}/{date}", endpoint, page, date)
                .retrieve()
                .bodyToMono(String.class)
                .flatMapMany(json -> {
                    List<PatentDto> patents = decodeUnicode(json);
                    return Flux.fromIterable(patents);
                })
                .flatMap(dto -> {
                    // Call fetchAndAttachImage only if endpoint is "select_tzizo"
                    if ("select_tzizo".equals(endpoint)) {
                        return fetchAndAttachImage(dto, "select_tz_patent_image");
                    } else {
                        return Mono.just(dto); // Simply return the dto without modifying it
                    }
                })
                .doOnError(error -> logger.error("Error fetching patents for {} on {}: {}", endpoint, date, error.getMessage()));
    }


    public Mono<LinkedHashMap<Integer, String>> fetchDatesForYear(int year) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/bulletin/select_bull_list_published")
                        .queryParam("year", year)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .map(json -> {
                    try {
                        // Deserialize JSON into a regular Map
                        Map<String, String> dateMap = objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});

                        // Convert it into a LinkedHashMap with Integer keys
                        LinkedHashMap<Integer, String> orderedMap = new LinkedHashMap<>();
                        dateMap.forEach((key, value) -> orderedMap.put(Integer.parseInt(key), value));

                        return orderedMap;
                    } catch (JsonProcessingException e) {
                        logger.error("Error decoding dates JSON response: {}", json, e);
                        throw new RuntimeException("Error decoding dates JSON response", e);
                    }
                })
                .doOnError(error -> logger.error("Error fetching dates for year {}: {}", year, error.getMessage()));
    }





    private List<PatentDto> decodeUnicode(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<PatentDto>>() {});
        } catch (JsonProcessingException e) {
            logger.error("Error decoding Unicode JSON response: {}", json, e);
            throw new RuntimeException("Error decoding Unicode JSON response", e);
        }
    }

    /**
     * Fetches the image for a patent and attaches it as a Base64 string.
     */
    private Mono<PatentDto> fetchAndAttachImage(PatentDto patent, String endpoint) {
        String imageEndpoint = endpoint + "/" + patent.getId() + "?eksp=false";
        return webClient.get()
                .uri("/bulletin/"+imageEndpoint)
                .retrieve()
                .bodyToMono(byte[].class)
                .map(imageBytes -> {
                    String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                    patent.setImageBase64(base64Image);
                    logger.info("Fetched image for patent ID: {}", patent.getId());
                    return patent;
                })
                .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                    return Mono.just(patent);
                })
                .onErrorResume(e -> {
                    // Log other errors but continue processing
                    return Mono.just(patent);
                });
    }
}
