package kz.it.patentparser.parser;

import kz.it.patentparser.dto.PatentDto;
import kz.it.patentparser.model.Patent;
import kz.it.patentparser.model.PatentAdditionalField;
import kz.it.patentparser.service.PatentApiClient;
import kz.it.patentparser.service.PatentService;
import kz.it.patentparser.validator.PatentValidator;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class EbulletinPatentParser implements PatentParser {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final Logger logger = LoggerFactory.getLogger(EbulletinPatentParser.class);
    private static final String MAIN_URL = "https://qazpatent.kz/ru/electronic-bulletin";
    private static final String BASE_URL = "ebulletin.kazpatent.kz";
    private static final Pattern YEAR_PATTERN = Pattern.compile("targetYear=(\\d{4})");

    private final PatentValidator validator;
    private final PatentService patentService;
    private final PatentApiClient patentApiClient;

    public EbulletinPatentParser(PatentService patentService, PatentValidator validator, PatentApiClient patentApiClient) {
        this.patentService = patentService;
        this.validator = validator;
        this.patentApiClient = patentApiClient;
    }

    @Override
    public List<Patent> parseAll(String from, boolean both) {
        List<Patent> patents = new ArrayList<>();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--no-sandbox");
        WebDriver driver = new ChromeDriver(options);

        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

            logger.info("Starting patent parsing process...");
            driver.get("https://qazpatent.kz/ru/electronic-bulletin");
            logger.info("Opened Ebulleting Kazpatent website.");

            WebElement contentDiv = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".content-m.content-default")));

            List<WebElement> links = contentDiv.findElements(By.tagName("a"));
            List<String> yearLinks = new ArrayList<>();

            for (WebElement link : links) {
                String href = link.getAttribute("href");
                Matcher matcher = YEAR_PATTERN.matcher(href);

                if (matcher.find()) {
                    yearLinks.add(href);
                }
            }

            for (String yearLink : yearLinks) {
                driver.get(yearLink);
                logger.info("Opened Ebulletin year page: " + yearLink);

                parseFromYear(wait, driver);

                driver.navigate().back(); // Возвращаемся обратно к списку годов
                logger.info("Returned to the year list page.");
            }

        } catch (Exception e) {
            logger.error("Error parsing Ebulletin: " + e.getMessage());
            e.printStackTrace();
        } finally {
            driver.quit();
        }
        return patents;
    }

    private void processYear(int year, String href, WebDriver driver, WebDriverWait wait) {
        driver.get(href);
        logger.info("Opened Ebulletin year: " + year);

        parseFromYear(wait, driver);
    }
    private Map<String, String> getCategories() {
        Map<String, String> categories = new LinkedHashMap<>();
        categories.put("Изобретения", "select_iz_patent");
        categories.put("Полезная модели", "select_pm_patent");
        categories.put("Товарные знаки", "select_tzizo");
        return categories;
    }
    private void parseFromYear(WebDriverWait wait, WebDriver driver) {
        int page = 1; // Persist across all dates
        List<WebElement> contentRows;

        try {
            contentRows = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("content-row")));

            for (int i = 0; i < contentRows.size(); i++) {
                boolean success = false;
                int retryCount = 0;

                while (!success && retryCount < 3) { // Retry logic for stale elements
                    try {
                        // Re-locate contentRows to avoid StaleElementException
                        contentRows = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("content-row")));
                        WebElement row = contentRows.get(i);

                        WebElement dateElement = row.findElement(By.cssSelector("div.pdf-content.align-center"));
                        String date = dateElement.getText().substring(1, 11);
                        logger.info("Processing date: {} (Starting from page: {})", date, page);

                        boolean foundDataOnAnyPage = false;

                        while (true) { // Keep fetching pages until no data is found on a page
                            boolean foundDataThisPage = false;

                            for (Map.Entry<String, String> entry : getCategories().entrySet()) {
                                String category = entry.getKey();
                                String categoryValue = entry.getValue();

                                logger.info("Processing category: {} on page: {}", category, page);

                                try {
                                    List<Patent> patents = patentApiClient.fetchPatents(categoryValue, page, date)
                                            .map(dto -> convertToEntity(dto, category))
                                            .collect(Collectors.toList())
                                            .block();

                                    if (patents != null && !patents.isEmpty()) {
                                        logger.info("Fetched {} patents for category: {} on page: {}", patents.size(), category, page);
                                        savePatentData(patents);
                                        foundDataOnAnyPage = true; // At least one page had data for this date
                                        foundDataThisPage = true; // Data was found on this specific page
                                    } else {
                                        logger.warn("No patents found for category: {} on date: {} (Page {})", category, date, page);
                                    }

                                } catch (WebClientResponseException e) {
                                    logger.error("HTTP error while fetching patents for category: {} on date: {} - Status: {} - Response: {}",
                                            category, date, e.getStatusCode(), e.getResponseBodyAsString(), e);
                                } catch (Exception e) {
                                    logger.error("Unexpected error while fetching patents for category: {} on date: {}", category, date, e);
                                }
                            }

                            if (foundDataThisPage) {
                                page++; // Move to next page if data was found
                            } else {
                                break; // No data on this page, stop searching for this date
                            }
                        }

                        logger.info("Finished processing date: {} (Last processed page: {})", date, page);
                        success = true; // Mark as successfully processed

                        if (!foundDataOnAnyPage) {
                            logger.warn("No data found for entire date: {}. Moving to the next date.", date);
                        }

                    } catch (StaleElementReferenceException e) {
                        logger.warn("Stale element encountered. Retrying... Attempt {}/3", retryCount + 1);
                        retryCount++;
                        Thread.sleep(1000); // Small delay before retrying
                    }
                }
            }

        } catch (TimeoutException e) {
            logger.error("Timeout while waiting for content rows: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error in parsing: {}", e.getMessage(), e);
        }
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
        if(dto.getDate85()!=null){
            additionalFields.add(new PatentAdditionalField(patent, "date_85", dto.getDate85()));
        }
        if(dto.getField86()!=null){
            additionalFields.add(new PatentAdditionalField(patent, "field_86", dto.getField86()));
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

    private String getTextByCss(WebElement element, String cssSelector) {
        try {
            return element.findElement(By.cssSelector(cssSelector)).getText().trim();
        } catch (Exception e) {
            logger.warn("Error getting text by css: {}", e);
            return "";
        }
    }

    @Override
    public List<Patent> parseCategory(String category) {
        return new ArrayList<>();
    }

    @Override
    public List<Patent> parseFrom(String category, String from, boolean both) {
        return null;
    }
}
