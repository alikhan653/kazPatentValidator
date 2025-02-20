package kz.it.patentparser.service;

import kz.it.patentparser.model.Patent;
import kz.it.patentparser.repository.PatentRepository;
import kz.it.patentparser.validator.PatentValidator;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PatentParserService {

    private static final Logger logger = LoggerFactory.getLogger(PatentParserService.class);

    private final PatentRepository patentRepository;

    private final PatentValidator validator;


    public PatentParserService(PatentRepository patentRepository, PatentValidator validator) {
        this.patentRepository = patentRepository;
        this.validator = validator;
    }

    private boolean isPatentExists(Patent patent) {
        Optional<Patent> existingPatent = patentRepository.findByApplicationNumber(patent.getApplicationNumber());
        return existingPatent.isPresent();
    }

    public void parseAllCategories() {
        WebDriver webDriver = new ChromeDriver();
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(20));

        logger.info("Starting patent parsing process...");

        webDriver.get("https://gosreestr.kazpatent.kz/");
        logger.info("Opened KazPatent website.");

        Map<String, String> categories = new LinkedHashMap<>();
        categories.put("Изобретения", "cbReestrType_DDD_L_LBI1T0");
        categories.put("Полезные модели", "cbReestrType_DDD_L_LBI2T0");
        categories.put("Промышленные образцы", "cbReestrType_DDD_L_LBI3T0");
        categories.put("Селекционные достижения", "cbReestrType_DDD_L_LBI4T0");
        categories.put("Товарные знаки", "cbReestrType_DDD_L_LBI5T0");
        categories.put("Общеизвестные товарные знаки", "cbReestrType_DDD_L_LBI6T0");
        categories.put("Наименования мест происхождения товаров", "cbReestrType_DDD_L_LBI7T0");


        for (Map.Entry<String, String> category : categories.entrySet()) {
            try {
                logger.info("Processing category: {}", category.getKey());
                Thread.sleep(3000); // Wait for data to load
                selectCategory(webDriver, wait, category.getKey(), category.getValue());
                Thread.sleep(3000); // Wait for data to load
                parsePatents(webDriver, wait, category.getKey());
            } catch (Exception e) {
                logger.error("Error parsing category: {}", category.getKey(), e);
            }
        }

        logger.info("Patent parsing process completed.");
        webDriver.quit();
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

    private void parsePatents(WebDriver webDriver, WebDriverWait wait, String category) {
        logger.info("Parsing patents for category: {}", category);

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("cvReestr_DXMainTable")));
        logger.info("Search results loaded for category: {}", category);

        List<WebElement> patentCards = webDriver.findElements(By.cssSelector("div.dxcvFlowCard_Material"));

        if (patentCards.isEmpty()) {
            logger.warn("No patents found for category: {}", category);
            return;
        }

        for (WebElement card : patentCards) {
            try {
                logger.info("Card text: {}", card.getText());
                Patent patent = extractPatentData(card.getText(), category);

                // Проверка наличия в базе
                if (isPatentExists(patent)) {
                    logger.info("Patent already exists, skipping: {}", patent.getTitle());
                    continue;
                }

                // Проверка валидности патента
                if (validator.isValid(patent)) {
                    patentRepository.save(patent);
                    logger.info("Saved patent: {}", patent.getTitle());
                } else {
                    logger.warn("Invalid patent data, skipping: {}", patent);
                }
            } catch (NoSuchElementException e) {
                logger.error("Error extracting patent data for category: {}", category, e);
            }
        }
    }

    private Patent extractPatentData(String cardText, String category) {
        return new Patent(
                getFieldValue(cardText, "Название: "),
                getFieldValue(cardText, "Номер заявки: "),
                getFieldValue(cardText, "Дата подачи заявки: "),
                getFieldValue(cardText, "Автор(-ы): "),
                getFieldValue(cardText, "Патентообладатель: "),
                getFieldValue(cardText, "№ охранного документа: "),
                getFieldValue(cardText, "Статус: "),
                getFieldValue(cardText, "МПК: "),
                getFieldValue(cardText, "Номер бюллетеня: "),
                getFieldValue(cardText, "Дата бюллетеня: "),
                category
        );
    }

    private String getFieldValue(String card, String label) {
        String[] lines = card.split("\n");
        for (String line : lines) {
            if (line.startsWith(label)) {
                return line.substring(label.length()).trim();
            }
        }
        return "";
    }

}