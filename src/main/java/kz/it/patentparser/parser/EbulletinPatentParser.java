package kz.it.patentparser.parser;

import kz.it.patentparser.model.Patent;
import kz.it.patentparser.service.PatentService;
import kz.it.patentparser.validator.PatentValidator;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class EbulletinPatentParser implements PatentParser {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final Logger logger = LoggerFactory.getLogger(EbulletinPatentParser.class);

    private final PatentValidator validator;
    private final PatentService patentService;

    public EbulletinPatentParser(PatentService patentService, PatentValidator validator) {
        this.patentService = patentService;
        this.validator = validator;
    }

    @Override
    public List<Patent> parse() {
        try {
            WebDriver driver = new ChromeDriver();
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
            List<Patent> patents = new ArrayList<>();

            logger.info("Starting patent parsing process...");
            driver.get("https://ebulletin.kazpatent.kz");
            logger.info("Opened Ebulleting Kazpatent website.");

            wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("content-row")));

            // Find all content-row elements
            List<WebElement> contentRows = driver.findElements(By.className("content-row"));

            for (WebElement row : contentRows) {
                try {
                    // Find the bulletin-content align-left onhover div
                    WebElement clickableDiv = row.findElement(By.cssSelector("div.bulletin-content.align-left.onhover"));

                    // Use Actions class to simulate hover and click
                    Actions actions = new Actions(driver);
                    actions.moveToElement(clickableDiv).click().perform();

                    logger.info("Clicked on bulletin entry");
                    Thread.sleep(1000); // Small delay for UI response
                } catch (Exception e) {
                    logger.error("Error clicking on bulletin entry: " + e.getMessage());
                }
            }
            logger.info("Patent parsing process completed.");
            driver.quit();
            return patents;
        } catch (Exception e) {
            logger.error("Error parsing patents: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}
