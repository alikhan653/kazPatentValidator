package kz.it.patentparser.util;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

public class ImageScraper {

    public static String captureImageBase64(String patentUrl) {
        System.setProperty("webdriver.chrome.driver", "C:\\Users\\user\\Downloads\\chromedriver-win64\\chromedriver.exe");

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");  // Run Chrome in headless mode (no GUI)
        WebDriver driver = new ChromeDriver(options);

        try {
            driver.get(patentUrl);
            Thread.sleep(3000); // Wait for page to load

            WebElement imgElement = driver.findElement(By.cssSelector("div.plan_img5 img, div.plan_img img"));
            File screenshot = imgElement.getScreenshotAs(OutputType.FILE);
            byte[] fileContent = Files.readAllBytes(screenshot.toPath());

            return Base64.getEncoder().encodeToString(fileContent);
        } catch (Exception e) {
            System.err.println("Error capturing image: " + e.getMessage());
            return null;
        } finally {
            driver.quit();
        }
    }
}
