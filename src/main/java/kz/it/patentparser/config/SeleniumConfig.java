package kz.it.patentparser.config;

import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.annotation.PreDestroy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SeleniumConfig {

    private WebDriver webDriver;

    @Bean
    public WebDriver webDriver() {
        if (webDriver == null) {
            System.setProperty("webdriver.chrome.driver", "C:\\Users\\user\\Downloads\\chromedriver-win64\\chromedriver.exe");
            // WebDriverManager.chromedriver().setup(); // Use this if you prefer auto-management

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless");  // Run without UI
            options.addArguments("--disable-gpu");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");

            webDriver = new ChromeDriver(options);
        }
        return webDriver;
    }

    @PreDestroy
    public void closeWebDriver() {
        if (webDriver != null) {
            webDriver.quit();
        }
    }
}
