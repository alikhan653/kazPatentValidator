package kz.it.patentparser.service;

import kz.it.patentparser.dto.PatentDto;
import kz.it.patentparser.model.Patent;
import kz.it.patentparser.model.PatentAdditionalField;
import kz.it.patentparser.parser.EbulletinPatentParser;
import kz.it.patentparser.repository.PatentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Service
public class PatentFetcherService {

    private static final String BASE_URL = "https://ebulletin.kazpatent.kz:6002/bulletin/published";
    private static final List<Integer> YEARS = IntStream.rangeClosed(2018, 2025).boxed().toList();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final Logger logger = LoggerFactory.getLogger(EbulletinPatentParser.class);

    private static final Map<String, String> CATEGORIES = Map.of(
            "Изобретения", "select_iz_patent",
            "Полезная модели", "select_pm_patent",
            "Товарные знаки", "select_tzizo"
    );

    private final WebClient webClient;
    private final PatentRepository patentRepository;

    public PatentFetcherService(WebClient.Builder webClientBuilder, PatentRepository patentRepository) {
        this.webClient = webClientBuilder.baseUrl(BASE_URL).build();
        this.patentRepository = patentRepository;
    }

    public void fetchPatents() {
        Flux.fromIterable(YEARS)
                .flatMap(this::fetchDatesForYear)
                .flatMap(entry -> fetchPatentsByCategory(entry.getKey(), entry.getValue())
                        .map(dto -> Map.entry(entry.getKey(), dto))) // Preserve category
                .flatMap(entry -> addTrademarkImageIfNeeded(entry.getKey(), entry.getValue())
                        .map(dto -> Map.entry(entry.getKey(), dto))) // Preserve category
                .map(entry -> convertToEntity(entry.getValue(), entry.getKey())) // Convert DTO to entity
                .flatMap(patent -> Mono.fromCallable(() -> patentRepository.save(patent))
                        .subscribeOn(Schedulers.boundedElastic())) // Run on a separate thread
                .doOnComplete(() -> logger.info("✅ Patent fetching process completed."))
                .subscribe(
                        result -> logger.info("✅ Saved patent"),
                        error -> logger.error("❌ Error processing patents", error)
                );
    }

    /**
     * Fetches available bulletin dates and indexes for a given year.
     */
    private Flux<Map.Entry<String, String>> fetchDatesForYear(Integer year) {
        String url = BASE_URL + "/select_bull_list_published?year=" + year;
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {})
                .flatMapMany(map -> Flux.fromIterable(map.entrySet()))
                .doOnError(e -> logger.error("❌ Failed to fetch dates for year {}", year, e));
    }

    /**
     * Fetches patents for a specific category and date.
     */
    private Flux<PatentDto> fetchPatentsByCategory(String date, String index) {
        return Flux.fromIterable(CATEGORIES.entrySet())
                .flatMap(entry -> {
                    String categoryName = entry.getKey();
                    String categoryUrl = entry.getValue();
                    String url = BASE_URL + "/" + categoryUrl + "/" + index + "/" + date;

                    return webClient.get()
                            .uri(url)
                            .retrieve()
                            .bodyToFlux(PatentDto.class)
                            .map(dto -> {
                                dto.setCategory(categoryName); // Assign category inside DTO
                                return dto;
                            })
                            .doOnError(e -> logger.error("❌ Failed to fetch patents for category: {} on date: {}", categoryName, date, e));
                });
    }

    /**
     * Adds base64 image data to trademarks.
     */
    private Mono<PatentDto> addTrademarkImageIfNeeded(String category, PatentDto patent) {
        if (!"Товарные знаки".equals(category) || patent.getId() == null) {
            return Mono.just(patent);
        }

        String imageUrl = BASE_URL + "/select_tz_patent_image/" + patent.getId();

        return webClient.get()
                .uri(imageUrl)
                .retrieve()
                .bodyToMono(String.class)
                .map(imageBase64 -> {
                    patent.setImageBase64(imageBase64);
                    return patent;
                })
                .doOnError(e -> logger.error("❌ Failed to fetch image for trademark ID: {}", patent.getId(), e))
                .onErrorReturn(patent); // If error, return patent without image
    }

    /**
     * Converts a DTO to an entity.
     */
    private Patent convertToEntity(PatentDto dto, String category) {
        Patent patent = new Patent();
        patent.setPatentSite(BASE_URL);
        patent.setCategory(category);
        patent.setSecurityDocNumber(dto.getSecurityDocNumber());
        patent.setApplicationNumber(dto.getApplicationNumber());
        patent.setFilingDate(LocalDate.parse(dto.getApplicationDate(), DATE_FORMATTER));
        patent.setBulletinNumber(dto.getBulletinNumber());
        patent.setBulletinDate(LocalDate.parse(dto.getPublicationDate(), DATE_FORMATTER));
        patent.setIpc(dto.getIpcCodes());
        patent.setAuthors(dto.getAuthorsRu());
        patent.setOwner(dto.getOwnerRu());
        patent.setTitle(dto.getTitleRu());

        List<PatentAdditionalField> additionalFields = new ArrayList<>();
        if (dto.getCode13() != null) additionalFields.add(new PatentAdditionalField(patent, "code_13", dto.getCode13()));
        if (dto.getField31() != null) additionalFields.add(new PatentAdditionalField(patent, "field_31", dto.getField31()));
        if (dto.getField32() != null) additionalFields.add(new PatentAdditionalField(patent, "field_32", dto.getField32()));
        if (dto.getField33() != null) additionalFields.add(new PatentAdditionalField(patent, "field_33", dto.getField33()));
        if (dto.getDate85() != null) additionalFields.add(new PatentAdditionalField(patent, "date_85", dto.getDate85()));
        if (dto.getField86() != null) additionalFields.add(new PatentAdditionalField(patent, "field_86", dto.getField86()));
        if (dto.getDescription() != null) additionalFields.add(new PatentAdditionalField(patent, "referat", dto.getDescription()));
        if (dto.getImageBase64() != null && !dto.getImageBase64().isEmpty()) {
            additionalFields.add(new PatentAdditionalField(patent, "image_base64", dto.getImageBase64()));
        }

        patent.setAdditionalFields(additionalFields);
        return patent;
    }
}
