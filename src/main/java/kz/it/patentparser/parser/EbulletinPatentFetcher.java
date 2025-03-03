package kz.it.patentparser.parser;

import kz.it.patentparser.dto.PatentDto;
import kz.it.patentparser.model.Patent;
import kz.it.patentparser.model.PatentAdditionalField;
import kz.it.patentparser.service.PatentApiClient;
import kz.it.patentparser.service.PatentService;
import kz.it.patentparser.validator.PatentValidator;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class EbulletinPatentFetcher implements PatentParser{
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final Logger logger = LoggerFactory.getLogger(EbulletinPatentParser.class);
    private static final String MAIN_URL = "ebulletin.kazpatent.kz";
    private static final String BASE_URL = "https://ebulletin.kazpatent.kz:6002/bulletin/published";
    private static final Pattern YEAR_PATTERN = Pattern.compile("targetYear=(\\d{4})");
    private static final String IMAGE_SAVE_DIR = "C:\\Users\\user\\OneDrive - International Information Technology University\\Рисунки\\patentImages\\";

    private final PatentValidator validator;
    private final PatentService patentService;
    private final PatentApiClient patentApiClient;
    private final WebClient webClient;


    public EbulletinPatentFetcher(PatentService patentService, PatentValidator validator, PatentApiClient patentApiClient, WebClient.Builder webClientBuilder) {
        this.patentService = patentService;
        this.validator = validator;
        this.patentApiClient = patentApiClient;
        this.webClient = webClientBuilder.baseUrl(BASE_URL).build();

    }

    @Override
    public List<Patent> parseAll(String from, boolean both) {
        List<Patent> patents = new ArrayList<>();
//        ChromeOptions options = new ChromeOptions();
//        options.addArguments("--disable-dev-shm-usage");
//        options.addArguments("--no-sandbox");
//        WebDriver driver = new ChromeDriver(options);

        try {
//            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

            logger.info("Starting patent fetching process...");
//            driver.get("https://qazpatent.kz/ru/electronic-bulletin");
//            logger.info("Opened Ebulleting Kazpatent website.");

//            WebElement contentDiv = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".content-m.content-default")));

//            List<WebElement> links = contentDiv.findElements(By.tagName("a"));
            List<String> years = Arrays.asList("2018", "2019", "2020", "2021", "2022", "2023", "2024", "2025");


            for (String year : years) {
                logger.info("Processing year: {}", year);
                String url = BASE_URL + "/select_bull_list_published?year=" + year;
                LinkedHashMap<Integer, String> dates = patentApiClient.fetchDatesForYear(Integer.parseInt(year)).block();

                if (dates == null || dates.isEmpty()) {
                    logger.warn("No dates found for year: {}", year);
                    continue;
                }

                for (Map.Entry<Integer, String> date : dates.entrySet()) {
                    for (Map.Entry<String, String> entry : getCategories().entrySet()) {
                        String category = entry.getKey();
                        String categoryValue = entry.getValue();

                        logger.info("Processing category: {} on index {} and date {}", category, date.getKey(), date.getValue());

                        try {
                            patents = patentApiClient.fetchPatents(categoryValue, date.getKey(), date.getValue())
                                    .map(dto -> convertToEntity(dto, category))
                                    .collect(Collectors.toList())
                                    .block();

                            if (patents != null && !patents.isEmpty()) {
                                logger.info("Fetched {} patents for category: {} on index: {}", patents.size(), category, date.getKey());
                                savePatentData(patents);
                            } else {
                                logger.warn("No patents found for category: {} on date: {} (Page {})", category, date, date.getKey());
                            }

                        } catch (WebClientResponseException e) {
                            logger.error("HTTP error while fetching patents for category: {} on date: {} - Status: {} - Response: {}",
                                    category, date.getValue(), e.getStatusCode(), e.getResponseBodyAsString(), e);
                        } catch (Exception e) {
                            logger.error("Unexpected error while fetching patents for category: {} on date: {}", category, date.getValue(), e);
                        }
                    }
                }
                logger.info("Fetched {} dates for year: {}, {}", dates.size(), year, dates);
            }


        } catch (Exception e) {
            logger.error("Error parsing Ebulletin: " + e.getMessage());
            e.printStackTrace();
        }
        return patents;
    }

    private Map<String, String> getCategories() {
        Map<String, String> categories = new LinkedHashMap<>();
        categories.put("Изобретения", "select_iz_patent");
        categories.put("Полезная модели", "select_pm_patent");
        categories.put("Товарные знаки", "select_tzizo");
        return categories;
    }

    private void savePatentData(List<Patent> patents) {
        logger.info("Saving patents to database...");
        patentService.savePatents(patents, logger);
        logger.info("Saved {} patents to database.", patents.size());

        List<PatentAdditionalField> additionalFields = patents.stream()
                .filter(patent -> patent.getAdditionalFields() != null) // Avoid null values
                .flatMap(patent -> patent.getAdditionalFields().stream())
                .toList();


        if (!additionalFields.isEmpty()) {
            patentService.saveAllAdditionalFields(additionalFields);
            logger.info("Saved {} additional fields to database.", additionalFields.size());
        } else {
            logger.info("No additional fields found to save.");
        }
    }


    private Patent convertToEntity(PatentDto dto, String category) {
        Patent patent = new Patent();
        patent.setPatentSite(MAIN_URL);
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
//        patent.setImageUrl(saveImage(dto.getImageBase64(), Long.valueOf(patent.getSecurityDocNumber())));

        List<PatentAdditionalField> additionalFields = new ArrayList<>();
        if(dto.getCode13()!=null){
            additionalFields.add(new PatentAdditionalField(patent, "code_13", dto.getCode13()));
        }
        if(dto.getField31()!=null){
            additionalFields.add(new PatentAdditionalField(patent, "field_31", dto.getField31()));
        }
        if(dto.getField32()!=null){
            additionalFields.add(new PatentAdditionalField(patent, "field_32", dto.getField32()));
        }
        if(dto.getField33()!=null){
            additionalFields.add(new PatentAdditionalField(patent, "field_33", dto.getField33()));
        }
        if(dto.getDescription()!=null){
            additionalFields.add(new PatentAdditionalField(patent, "description", dto.getField33()));
        }
        if(dto.getDate85()!=null){
            additionalFields.add(new PatentAdditionalField(patent, "date_85", dto.getDate85()));
        }
        if(dto.getField86()!=null){
            additionalFields.add(new PatentAdditionalField(patent, "field_86", dto.getField86()));
        }
        if(dto.getField181()!=null){
            additionalFields.add(new PatentAdditionalField(patent, "field_181", dto.getField86()));
        }
        if(dto.getField730Ru()!=null){
            additionalFields.add(new PatentAdditionalField(patent, "field_730", dto.getField86()));
        }
        if(dto.getField526Ru()!=null){
            additionalFields.add(new PatentAdditionalField(patent, "field_526", dto.getField86()));
        }
        if(dto.getField591()!=null){
            additionalFields.add(new PatentAdditionalField(patent, "field_591", dto.getField86()));
        }
        if(dto.getField510511()!=null){
            additionalFields.add(new PatentAdditionalField(patent, "field_510", dto.getField86()));
        }
        if(dto.getDescription()!=null){
            additionalFields.add(new PatentAdditionalField(patent, "referat", dto.getDescription()));
        }

        String patentImage = dto.getImageBase64();
        if(patentImage != null && !patentImage.isEmpty()){
            additionalFields.add(new PatentAdditionalField(patent, "image_base64", patentImage));
        }

        patent.setAdditionalFields(additionalFields);
        return patent;
    }


    private String saveImage(String base64Image, Long patentId) {
        // Ensure directory exists
        try {
            File directory = new File(IMAGE_SAVE_DIR);
            if (!directory.exists()) {
                directory.mkdirs(); // Create directories if they don’t exist
            }

            // Define the file path and name
            String fileName = "patent_" + patentId + ".png";
            File file = new File(IMAGE_SAVE_DIR + fileName);

            // Decode Base64 and write to file
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            Path filePath = Paths.get(IMAGE_SAVE_DIR, fileName);
            Files.write(filePath, imageBytes); // Save file

            // Return the relative path
            return Paths.get((IMAGE_SAVE_DIR), fileName).toString();
        } catch (IOException e) {
            logger.error("Error saving image for patent: {}", patentId, e);
            return null;
        } catch (Exception e) {
            logger.error("Unexpected error saving image for patent: {}", patentId, e);
            return null;
        }
    }

    @Override
    public List<Patent> parseCategory(String category) {
        return null;
    }

    @Override
    public List<Patent> parseFrom(String category, String from, boolean both) {
        return null;
    }
}
