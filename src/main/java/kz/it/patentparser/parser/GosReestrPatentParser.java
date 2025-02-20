package kz.it.patentparser.parser;

import kz.it.patentparser.model.Patent;
import kz.it.patentparser.model.PatentAdditionalField;
import kz.it.patentparser.parser.PatentParser;
import kz.it.patentparser.service.PatentService;
import kz.it.patentparser.validator.PatentValidator;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GosReestrPatentParser implements PatentParser {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final Logger logger = LoggerFactory.getLogger(GosReestrPatentParser.class);

    private final PatentValidator validator;
    private final PatentService patentService;

    @Autowired
    public GosReestrPatentParser(PatentService patentService, PatentValidator validator) {
        this.patentService = patentService;
        this.validator = validator;
    }

    @Override
    public List<Patent> parse() {
        WebDriver webDriver = new ChromeDriver();
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(20));
        List<Patent> patents = new ArrayList<>();

        logger.info("Starting patent parsing process...");
        webDriver.get("https://gosreestr.kazpatent.kz/");
        logger.info("Opened KazPatent website.");

        Map<String, String> categories = getCategories();

        for (Map.Entry<String, String> category : categories.entrySet()) {
            try {
                logger.info("Processing category: {}", category.getKey());
                Thread.sleep(3000);
                selectCategory(webDriver, wait, category.getKey(), category.getValue());
                Thread.sleep(3000);
                patents.addAll(parsePatents(webDriver, wait, category.getKey()));
            } catch (Exception e) {
                logger.error("Error parsing category: {}", category.getKey(), e);
            }
        }

        logger.info("Patent parsing process completed.");
        webDriver.quit();
        return patents;
    }

    private Map<String, String> getCategories() {
        Map<String, String> categories = new LinkedHashMap<>();
        categories.put("Изобретения", "cbReestrType_DDD_L_LBI1T0");
        categories.put("Полезные модели", "cbReestrType_DDD_L_LBI2T0");
        categories.put("Селекционные достижения", "cbReestrType_DDD_L_LBI4T0");
        categories.put("Товарные знаки", "cbReestrType_DDD_L_LBI5T0");
        categories.put("Общеизвестные товарные знаки", "cbReestrType_DDD_L_LBI6T0");
        return categories;
    }

    private void selectCategory(WebDriver webDriver, WebDriverWait wait, String categoryName, String categoryId) {
        try {
            logger.info("Selecting category: {}", categoryName);
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("LoadingPanel_LD")));

            WebElement dropdownButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("cbReestrType_B-1")));
            dropdownButton.click();
            logger.debug("Dropdown opened for category selection.");

            WebElement categoryOption = wait.until(ExpectedConditions.elementToBeClickable(By.id(categoryId)));
            categoryOption.click();

            wait.until(ExpectedConditions.textToBePresentInElementValue(By.id("cbReestrType_I"), categoryName));

            WebElement searchButton = webDriver.findElement(By.id("btnSearch"));
            searchButton.click();
            logger.info("Search button clicked for category: {}", categoryName);

            logger.info("Successfully selected category: {}", categoryName);
        } catch (TimeoutException e) {
            logger.error("Timeout while selecting category: {}", categoryName, e);
        } catch (NoSuchElementException e) {
            logger.error("Could not find dropdown elements for category: {}", categoryName, e);
        }
    }
    private List<Patent> parsePatents(WebDriver webDriver, WebDriverWait wait, String category) {
        List<Patent> patents = new ArrayList<>();

        logger.info("Parsing patents for category: {}", category);

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("cvReestr_DXMainTable")));
        logger.info("Search results loaded for category: {}", category);

        List<WebElement> patentCards = webDriver.findElements(By.cssSelector("div.dxcvFlowCard_Material"));

        if (patentCards.isEmpty()) {
            logger.warn("No patents found for category: {}", category);
            return patents;
        }

        for (WebElement card : patentCards) {
            try {
                logger.info("Card text: {}", card.getText());

                Patent patent = extractPatentData(card.getText(), category);
                if (patentService.isPatentExists(patent)) {
                    logger.info("Patent already exists, skipping: {}", patent.getTitle());
                    continue;
                }
                if (validator.isValid(patent)) {
                    patentService.savePatent(patent);
                    saveAdditionalFields(patent, card.getText());
                    patents.add(patent);
                    logger.info("Saved patent: {}", patent.getStatus());
                } else {
                    logger.warn("Invalid patent data, skipping: {}", patent);
                }
            } catch (NoSuchElementException e) {
                logger.error("Error extracting patent data for category: {}", category, e);
            }
        }
        return patents;
    }

    private Patent extractPatentData(String cardText, String category) {
        Patent patent = new Patent();
        patent.setCategory(category);

        // Extracting common fields
        patent.setSecurityDocNumber(getFieldValue(cardText, "№ охранного документа:?"));
        patent.setRegistrationNumber(getFieldValue(cardText, "№ регистрации:?"));
        patent.setStatus(getFieldValue(cardText, "Статус:?"));
        patent.setApplicationNumber(getFieldValue(cardText, "Номер заявки:?"));
        patent.setFilingDate(getDateField(cardText, "Дата подачи заявки:?"));
        patent.setRegistrationDate(getDateField(cardText, "Дата регистрации:?"));
        patent.setExpirationDate(getDateField(cardText, "Срок действия:?"));
        patent.setTitle(getFieldValue(cardText, "Название:?|Наименование сорта, породы:?"));
        patent.setIpc(getFieldValue(cardText, "МПК:?|МКПО:?"));
        patent.setBulletinNumber(getFieldValue(cardText, "Номер бюллетеня:?"));
        patent.setBulletinDate(getDateField(cardText, "Дата бюллетеня:?"));
        patent.setSortName(getFieldValue(cardText, "Наименование сорта, породы:?"));
        patent.setPatentHolder(getFieldValue(cardText, "Патентообладатель:?"));
        patent.setAuthors(getFieldValue(cardText, "Автор\\(-ы\\)?:?"));
        patent.setOwner(getFieldValue(cardText, "Владелец:?"));

        return patent;
    }

    private void saveAdditionalFields(Patent patent, String cardText) {
        String[] lines = cardText.split("\n");

        for (String line : lines) {
            String[] parts = line.split(":", 2);
            if (parts.length == 2) {
                String label = parts[0].trim();
                String value = parts[1].trim();

                if (!isCoreField(label)) {
                    PatentAdditionalField additionalField = new PatentAdditionalField(patent, label, value);
                    patentService.saveAdditionalFields(additionalField);
                }
            }
        }
    }

    private boolean isCoreField(String label) {
        return List.of("Название", "Номер заявки", "Дата подачи заявки", "Автор(-ы)", "Патентообладатель",
                "№ охранного документа", "Статус", "МПК", "Номер бюллетеня", "Дата бюллетеня").contains(label);
    }

    private String getFieldValue(String card, String regex) {
        Pattern pattern = Pattern.compile(regex + "\\s*:?\\s*(.*)"); // Поддержка ":" и пробела
        Matcher matcher = pattern.matcher(card);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private LocalDate getDateField(String card, String regex) {
        Pattern pattern = Pattern.compile(regex + "\\s*:?\\s*(\\d{2}\\.\\d{2}\\.\\d{4})");
        Matcher matcher = pattern.matcher(card);
        if (matcher.find()) {
            String dateStr = matcher.group(1).trim();
            try {
                return LocalDate.parse(dateStr, DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                System.err.println("Error parsing date: " + dateStr);
            }
        }
        return null;
    }
}
