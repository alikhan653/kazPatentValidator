package kz.it.patentparser.util;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Base64;

public class ImageScraper {

    public static String captureImageBase64(String patentUrl, Logger logger) {
        System.setProperty("webdriver.chrome.driver", "C:\\Users\\user\\Downloads\\chromedriver-win64\\chromedriver.exe");

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");  // Run Chrome in headless mode (no GUI)
        options.addArguments("--no-sandbox"); // Bypass OS security model
        options.addArguments("--disable-dev-shm-usage"); // Prevent crashes on low-memory environments
        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        try {
            driver.get(patentUrl);
            // Wait for the main container (ensuring page is loaded)
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("body")));

            WebElement imgElement = driver.findElements(By.cssSelector("div.plan_img5 img, div.plan_img img"))
                    .stream()
                    .filter(WebElement::isDisplayed)
                    .findFirst()
                    .orElse(null);

            File screenshot;

            if (imgElement != null && imgElement.isDisplayed()) {
                // Capture the image if found
                screenshot = imgElement.getScreenshotAs(OutputType.FILE);
                logger.info("Image captured successfully.");
            } else {
                // If no image, find and capture text section
                WebElement textElement = driver.findElements(By.cssSelector("div.col-lg-4 h3"))
                        .stream()
                        .filter(WebElement::isDisplayed)
                        .findFirst()
                        .orElse(null);

                if (textElement != null && textElement.isDisplayed()) {
                    screenshot = textElement.getScreenshotAs(OutputType.FILE);
                    logger.info("Text-based screenshot captured successfully.");
                } else {
                    logger.warn("No image or text found.");
                    return null;
                }
            }

            byte[] fileContent = Files.readAllBytes(screenshot.toPath());
            return Base64.getEncoder().encodeToString(fileContent);
        } catch (Exception e) {
            logger.error("Error capturing image: " + e.getMessage());
            return null;
        } finally {
            driver.quit();
        }
    }
}
