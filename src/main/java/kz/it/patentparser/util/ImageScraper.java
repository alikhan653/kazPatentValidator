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
import java.util.List;

public class ImageScraper {
    private static WebDriver driver;
    private static WebDriverWait wait;

    static {
        ChromeOptions options = new ChromeOptions();
//        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (driver != null) {
                driver.quit();
            }
        }));
    }


    public static synchronized String captureImageBase64(String patentUrl, Logger logger) {
        try {
            driver.get(patentUrl);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("body")));

            File screenshot = null;

            // Try to find the image element without waiting
            WebElement imgElement = driver.findElement(By.cssSelector("div.plan_img5 img, div.plan_img img"));
            if (imgElement.isDisplayed()) {
                screenshot = imgElement.getScreenshotAs(OutputType.FILE);
                logger.info("Image captured successfully.");
            } else {
                // If image is not visible, check for text
                WebElement textElement = driver.findElement(By.cssSelector("div.col-lg-4 h3"));
                if (textElement.isDisplayed()) {
                    screenshot = textElement.getScreenshotAs(OutputType.FILE);
                    logger.info("Text-based screenshot captured successfully.");
                }
            }

            if (screenshot == null) {
                logger.warn("No image or text found.");
                return null;
            }

            byte[] fileContent = Files.readAllBytes(screenshot.toPath());
            return Base64.getEncoder().encodeToString(fileContent);

        } catch (NoSuchElementException e) {
            logger.warn("No image or text found on the page.");
        } catch (TimeoutException e) {
            logger.error("Page elements did not load in time.", e);
        } catch (Exception e) {
            logger.error("Error capturing image", e);
        }
        return null;
    }


}
