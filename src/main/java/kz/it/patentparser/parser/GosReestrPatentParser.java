package kz.it.patentparser.parser;

import kz.it.patentparser.enums.NavigationDirection;
import kz.it.patentparser.model.Patent;
import kz.it.patentparser.model.PatentAdditionalField;
import kz.it.patentparser.service.ImageService;
import kz.it.patentparser.service.PatentService;
import kz.it.patentparser.service.PatentStorageService;
import kz.it.patentparser.util.ImageScraper;
import kz.it.patentparser.validator.PatentValidator;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static kz.it.patentparser.util.TransliterationUtil.fixMixedCharacters;

@Component
public class GosReestrPatentParser implements PatentParser {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final Logger logger = LoggerFactory.getLogger(GosReestrPatentParser.class);

    private final PatentValidator validator;
    private final PatentService patentService;
    private final ImageService imageService;

    private final PatentStorageService patentStorageService;

    @Autowired
    public GosReestrPatentParser(PatentService patentService, PatentValidator validator, PatentStorageService patentStorageService, ImageService imageService) {
        this.patentService = patentService;
        this.validator = validator;
        this.patentStorageService = patentStorageService;
        this.imageService = imageService;
    }

    @Override
    public List<Patent> parseAll(String from, boolean both) {
        List<Patent> patents = new ArrayList<>();
        System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver");
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--no-sandbox");
        options.addArguments("--headless");
        WebDriver webDriver = new ChromeDriver(options);

        try {
            WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(20));

            logger.info("Starting patent parsing process...");
            webDriver.get("https://gosreestr.kazpatent.kz/");
            logger.info("Opened KazPatent website.");

            Map<String, String> categories = getCategories();

            for (Map.Entry<String, String> category : categories.entrySet()) {
                try {
                    String categoryName = category.getKey();
                    String categoryId = category.getValue();

                    logger.info("Processing category: {}", categoryName);

                    boolean isCategorySelected = selectCategory(webDriver, wait, categoryName, categoryId);
                    if (!isCategorySelected) {
                        logger.error("Failed to select category: {}", categoryName);
                        continue;
                    }
                    if (!switchToTextView(webDriver, wait)) {
                        logger.error("Skipping category due to view switch failure: {}", categoryName);
                        continue;
                    }
                    Thread.sleep(3000);
                    setPageSizeTo200(webDriver, wait);
                    Thread.sleep(3000);
                    wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("LoadingPanel_LD")));
                    wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("cvReestr_LD")));
//                    if (!setFilterByDate(webDriver, wait)) {
//                        logger.error("Skipping category due to filter setup failure: {}", categoryName);
//                        continue;
//                    }
                    patents.addAll(parsePatentsWithPagination(webDriver, wait, category.getKey(), from, both));
                    logger.info("Parsed {} patents for category: {}", patents.size(), categoryName);
                } catch (Exception e) {
                    logger.error("Error parsing category: {}", category.getKey(), e);
                }
            }

            logger.info("Patent parsing process completed.");


            return patents;
        } finally {
            webDriver.quit(); // Ensures WebDriver is closed
        }
    }

    @Override
    public List<Patent> parseCategory(String category) {
        return parseFrom(category, NavigationDirection.NEXT.getClassName(), false);
    }

    @Override
    public List<Patent> parseFrom(String category, String from, boolean both) {
        List<Patent> patents = new ArrayList<>();
        System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver");
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--no-sandbox");
        options.addArguments("--headless");

        WebDriver webDriver = new ChromeDriver(options);
        try {
            WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(20));

            logger.info("Starting patent parsing process...");
            webDriver.get("https://gosreestr.kazpatent.kz/");
            logger.info("Opened KazPatent website.");

            String categoryId = getCategories().get(category);
            if (categoryId == null) {
                logger.error("Category not found: {}, {}", category, categoryId);
                return patents;
            }

            try {
                logger.info("Processing category: {}", category);

                logger.info("Processing category: {}", category);

                boolean isCategorySelected = selectCategory(webDriver, wait, category, categoryId);
                if (!isCategorySelected) {
                    logger.error("Failed to select category: {}", category);
                    return patents;
                }
                if (!switchToTextView(webDriver, wait)) {
                    logger.error("Skipping category due to view switch failure: {}", category);
                    return patents;
                }
                Thread.sleep(3000);
                setPageSizeTo200(webDriver, wait);
                Thread.sleep(3000);
//                if (!setFilterByDate(webDriver, wait)) {
//                    logger.error("Skipping category due to filter setup failure: {}", category);
//                    return patents;
//                }
                patents.addAll(parsePatentsWithPagination(webDriver, wait, category, from, both));
            } catch (Exception e) {
                logger.error("Error parsing category: {}", category, e);
            }

            logger.info("Patent parsing process completed.");
            return patents;
        } finally {
            webDriver.quit();
        }
    }

    private Map<String, String> getCategories() {
        Map<String, String> categories = new LinkedHashMap<>();
        categories.put("Селекционные достижения", "cbReestrType_DDD_L_LBI4T0");
        categories.put("Товарные знаки", "cbReestrType_DDD_L_LBI5T0");
        categories.put("Изобретения", "cbReestrType_DDD_L_LBI1T0");
        categories.put("Полезные модели", "cbReestrType_DDD_L_LBI2T0");
        categories.put("Общеизвестные товарные знаки", "cbReestrType_DDD_L_LBI6T0");
        return categories;
    }

    private boolean selectCategory(WebDriver webDriver, WebDriverWait wait, String categoryName, String categoryId) {
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
            return true;
        } catch (TimeoutException | NoSuchElementException e) {
            logger.error("Error while selecting category '{}': {}", categoryName, e.getMessage(), e);
            return false;
        }
    }

    private boolean stepToLastPage(WebDriver webDriver, WebDriverWait wait) {
        try {
            List<WebElement> paginationButtons = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                    By.cssSelector("a.dxp-num.dxRoundRippleTarget.dxRippleTarget")));
            WebElement lastPageButton = paginationButtons.get(paginationButtons.size() - 1);
            logger.info("Navigating to last page: {}", lastPageButton.getText());
            lastPageButton.click();
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("CVReestr_LD")));
            Thread.sleep(2000);
            logger.info("Navigated to last page.");
            return true;
        } catch (TimeoutException | NoSuchElementException | InterruptedException e) {
            logger.error("Error while navigating to last page: {}", e.getMessage(), e);
            return false;
        }
    }

    private int getLastPage(WebDriver webDriver, WebDriverWait wait) {
        try {
            List<WebElement> paginationButtons = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                    By.cssSelector("a.dxp-num.dxRoundRippleTarget.dxRippleTarget")));
            WebElement lastPageButton = paginationButtons.get(paginationButtons.size() - 1);
            return Integer.parseInt(lastPageButton.getText());
        } catch (TimeoutException e) {
            logger.error("Timeout while navigating to last page: {}", e);
        } catch (NoSuchElementException e) {
            logger.error("No pagination buttons found: {}", e);
        }
        return 1000;
    }

    private boolean setFilterByDate(WebDriver webDriver, WebDriverWait wait) {
        try {
            // Ищем элементы при каждой попытке
            List<WebElement> headerElements = webDriver.findElements(By.cssSelector("div.dxcvHeader_Material"));

            for (WebElement header : headerElements) {
                String text = header.getText().trim();

                if (text.contains("Дата регистрации") || text.contains("Дата подачи заявки")) {
                    try {
                        // Ожидаем, пока элемент не станет кликабельным
                        WebElement clickableHeader = wait.until(ExpectedConditions.elementToBeClickable(header));

                        // Кликаем на элемент
                        clickableHeader.click();

                        // Ожидаем изменения состояния, затем находим элемент снова
                        wait.until(ExpectedConditions.stalenessOf(clickableHeader));

                        // Снова находим элемент и кликаем второй раз
                        clickableHeader = wait.until(ExpectedConditions.elementToBeClickable(header));
                        clickableHeader.click();

                        logger.info("Clicked on filter header: {}", text);
                        return true;
                    } catch (StaleElementReferenceException e) {
                        logger.error("Элемент протух, пробую снова: {}", e.getMessage());
                        // Возможно, добавить дополнительные попытки, если необходимо
                    } catch (Exception e) {
                        logger.error("Ошибка при клике на фильтр для '{}': {}", text, e.getMessage(), e);
                        return false;
                    }
                }
            }

            logger.warn("Не найден фильтр по дате.");
            return false;

        } catch (TimeoutException | NoSuchElementException e) {
            logger.error("Error while setting date filter: {}", e.getMessage(), e);
            return false;
        }
    }


    private List<Patent> parsePatentsWithPagination(WebDriver webDriver, WebDriverWait wait, String category, String paginationId, boolean both) throws InterruptedException {
        List<Patent> patents = new ArrayList<>();
        int currentPage = 1;
        int stoppingPage = 0;
        if(NavigationDirection.PREVIOUS.getClassName().equals(paginationId)) {
            stepToLastPage(webDriver, wait);
            currentPage = getLastPage(webDriver, wait);
            stoppingPage = currentPage/2;
        } if(NavigationDirection.NEXT.getClassName().equals(paginationId)) {
            stoppingPage = getLastPage(webDriver, wait)/2;
        }

        JavascriptExecutor js = (JavascriptExecutor) webDriver;

        while (true) {
            try {

                scroll(webDriver, js, false);
                List<Patent> pagePatents = parsePatents(webDriver, wait, category, js);

                savePatentData(pagePatents);

                logger.info("Parsed {} patents on page: {}", pagePatents.size(), currentPage);

                // Wait until all buttons are visible
                List<WebElement> pageButtons = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(
                        By.cssSelector("a.dxp-button.dxp-bi.dxRoundRippleTarget.dxRippleTarget")));
                wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("cvReestr_LD")));

                Optional<WebElement> nextButton = pageButtons.stream()
                        .filter(button -> {
                            List<WebElement> images = button.findElements(By.tagName("img"));
                            return images.stream().anyMatch(img -> img.getAttribute("class").contains(paginationId));
                        })
                        .findFirst();

                if (nextButton.isPresent()) {
                    nextButton.get().click();
                    wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("LoadingPanel_LD")));
                    wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("cvReestr_LD")));
                    logger.info("Processed pages: {}", NavigationDirection.PREVIOUS.getClassName().equals(paginationId) ? currentPage-- : currentPage++);
                    if(both && NavigationDirection.PREVIOUS.getClassName().equals(paginationId) && currentPage <= stoppingPage) {
                        logger.info("Stopping pagination at page: {}", currentPage);
                        break;
                    }if(both && NavigationDirection.NEXT.getClassName().equals(paginationId) && currentPage >= stoppingPage) {
                        logger.info("Stopping pagination at page: {}", currentPage);
                        break;
                    }
                } else {
                    logger.info("No pagination button found. Stopping pagination.");
                    break;
                }
            } catch (NoSuchElementException | TimeoutException e) {
                logger.info("Pagination stopped. No more pages or timeout: " + e.getMessage());
                break;
            } catch (StaleElementReferenceException e) {
                logger.error("Stale element reference while navigating pages: {}", e);
            } catch (Exception e) {
                logger.error("Error parsing patents on page: {}", currentPage, e);
            }

        }
        return patents;
    }

    private List<Patent> parsePatents(WebDriver webDriver, WebDriverWait wait, String category, JavascriptExecutor js) throws InterruptedException {
        List<Patent> patents = new ArrayList<>();
        logger.debug("Parsing patents for category: {}", category);

        List<WebElement> patentCards = new ArrayList<>();
        int count = 3;
        while (count > 0) {
            try {
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("cvReestr_DXMainTable")));
                logger.debug("Search results loaded for category: {}", category);

                scroll(webDriver, js, true);

                patentCards = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(
                        By.cssSelector("div.dxcvFlowCard_Material")));
                break;
            } catch (TimeoutException e) {
                logger.error("Timeout while waiting for patent cards to load: {}", e.getMessage());
                break;
            } catch (NoSuchElementException e) {
                logger.error("No patent cards found for category: {}", category);
                break;
            } catch (StaleElementReferenceException e) {
                count--;
                if (count > 0) {
                    logger.warn("Stale element reference while waiting for patent cards to load. Retrying... Attempts left: {}", count);
                } else {
                    logger.error("Stale element reference persists, giving up: {}", e.getMessage());
                }
            }
        }

        if (patentCards.isEmpty()) {
            logger.warn("No patents found for category: {}", category);
            return patents;
        }

        for (int i = 0; i < patentCards.size(); i++) {
            WebElement card = patentCards.get(i);
            int retryCount = 0;
            boolean success = false;

            while (retryCount < 3 && !success) {
                try {
                    WebElement linkElement = retryFindElement(card, js, webDriver);
                    if(linkElement == null) {
                        logger.warn("No detailed link found for a patent. " + card.getText());
                        break;
                    }
                    String detailUrl = linkElement.getAttribute("href");

                    if (detailUrl == null || detailUrl.isEmpty()) {
                        logger.warn("No detailed link found for a patent.");
                        break;
                    }

                    Patent patent = extractPatentData(card.getText(), category);
                    logger.debug("Extracting patent data from card: {}", card.getText());

                    logger.debug("Fetching detailed patent page: {}", detailUrl);
                    Patent detailedPatent = fetchPatentDetails(detailUrl, category);

                    mergePatentData(patent, detailedPatent);

                    if (patentService.isPatentExists(patent)) {
                        logger.info("Patent already exists, skipping: {}",
                                patent.getSecurityDocNumber() != null ? patent.getSecurityDocNumber() : patent.getRegistrationNumber());
                        break;
                    }
                    String docNumber = (detailUrl.substring(detailUrl.lastIndexOf("=") + 1));

                    if (validator.isValid(patent)) {
                        patents.add(patent);
                        logger.info("Added patent: {}",
                                patent.getSecurityDocNumber() != null ? patent.getSecurityDocNumber() : patent.getRegistrationNumber());
                        patentStorageService.saveDocNumber(category, docNumber, true);
                    } else {
                        logger.warn("Invalid patent data, skipping: {}", patent);
                        patentStorageService.saveDocNumber(category, docNumber, false);
                        logger.info("Saving docNumber: {}", docNumber);
                    }

                    success = true;// If execution reaches here, parsing was successful
                } catch (StaleElementReferenceException e) {
                    logger.warn("StaleElementReferenceException occurred while parsing patent. Retrying {}/3...", retryCount + 1);
                    try {
                        // Re-fetch patent cards
                        patentCards = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(
                                By.cssSelector("div.dxcvFlowCard_Material")));
                        card = patentCards.get(i);
                    } catch (Exception retryEx) {
                        logger.error("Failed to re-fetch elements after StaleElementReferenceException: {}", retryEx.getMessage());
                        break; // Stop retrying if elements can't be retrieved
                    }
                    retryCount++;
                    Thread.sleep(1000); // Wait before retrying
                } catch (NoSuchElementException e) {
                    logger.error("Error extracting patent data for category: {}", category, e);
                    break;
                } catch (Exception e) {
                    logger.error("Error parsing patent for category: {}", category, e);
                    break;
                }
            }
        }
        return patents;
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

    private void scroll(WebDriver driver, JavascriptExecutor js, boolean scrollToBottom) throws InterruptedException {
        //retry if any exception occurs
        int attempts = 3;
        while (attempts > 0) {
            try {
                if (scrollToBottom) {
                    // Scroll to the bottom of the page
                    int lastHeight = ((Number) js.executeScript("return document.body.scrollHeight")).intValue();
                    while (true) {
                        js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
                        Thread.sleep(2000); // Wait for content to load

                        int newHeight = ((Number) js.executeScript("return document.body.scrollHeight")).intValue();
                        if (newHeight == lastHeight) {
                            break; // Stop when no new content is loaded
                        }
                        lastHeight = newHeight;
                    }
                } else {
                    // Scroll to the top of the page
                    js.executeScript("window.scrollTo(0, 0);");
                }
                Thread.sleep(3000);
                break;
            } catch (UnhandledAlertException e) {
                logger.error("Unhandled alert, retrying...");
                handleAlert(driver);
                Thread.sleep(3000);
            } catch (Exception e) {
                attempts--;
                logger.error("Error scrolling page, retrying..." + e);
                Thread.sleep(3000);
            }
        }
    }

    private void mergePatentData(Patent patent, Patent detailedPatent) {
        if (detailedPatent.getTitle() != null) {
            patent.setTitle(detailedPatent.getTitle());
        }
        if (detailedPatent.getStatus() != null) {
            patent.setStatus(detailedPatent.getStatus());
        }
        if (detailedPatent.getSecurityDocNumber() != null) {
            patent.setSecurityDocNumber(detailedPatent.getSecurityDocNumber());
        }
        if (detailedPatent.getRegistrationNumber() != null) {
            patent.setRegistrationNumber(detailedPatent.getRegistrationNumber());
        }
        if (detailedPatent.getApplicationNumber() != null) {
            patent.setApplicationNumber(detailedPatent.getApplicationNumber());
        }
        if (detailedPatent.getAuthors() != null) {
            patent.setAuthors(detailedPatent.getAuthors());
        }
        if (detailedPatent.getFilingDate() != null) {
            patent.setFilingDate(detailedPatent.getFilingDate());
        }
        if (detailedPatent.getRegistrationDate() != null) {
            patent.setRegistrationDate(detailedPatent.getRegistrationDate());
        }
        if (detailedPatent.getExpirationDate() != null) {
            patent.setExpirationDate(detailedPatent.getExpirationDate());
        }
        if (detailedPatent.getBulletinNumber() != null) {
            patent.setBulletinNumber(detailedPatent.getBulletinNumber());
        }
        if (detailedPatent.getBulletinDate() != null) {
            patent.setBulletinDate(detailedPatent.getBulletinDate());
        }
        if (detailedPatent.getIpc() != null) {
            patent.setIpc(detailedPatent.getIpc());
        }
        if (detailedPatent.getSortName() != null) {
            patent.setSortName(detailedPatent.getSortName());
        }
        if (detailedPatent.getPatentHolder() != null) {
            patent.setPatentHolder(detailedPatent.getPatentHolder());
        }
        if (detailedPatent.getOwner() != null) {
            patent.setOwner(detailedPatent.getOwner());
        }
        if(detailedPatent.getDocNumber() != null) {
            patent.setDocNumber(detailedPatent.getDocNumber());
        }
        if(detailedPatent.getImageUrl() != null) {
            patent.setImageUrl(detailedPatent.getImageUrl());
        }
        if (detailedPatent.getAdditionalFields() != null) {
            for (PatentAdditionalField field : detailedPatent.getAdditionalFields()) {
                field.setPatent(patent); // Важно!
            }
            patent.setAdditionalFields(detailedPatent.getAdditionalFields());
        }
    }

    private WebElement retryFindElement(WebElement card, JavascriptExecutor js, WebDriver driver) throws InterruptedException {
        int attempts = 3;
        while (attempts > 0) {
            try {
                return card.findElement(By.cssSelector("a"));
            } catch (StaleElementReferenceException e) {
                attempts--;
                logger.error("Stale element reference finding detailed link, retrying...");
                scroll(driver, js, false);
                Thread.sleep(3000);
                scroll(driver, js, true);
                Thread.sleep(3000);
            } catch (NoSuchElementException e) {
                logger.error("No detailed link found in card: {}", card.getText());
                return null;
            } catch (NoSuchSessionException e) {
                logger.error("Session expired, retrying...");
                return null;
            } catch (UnhandledAlertException e) {
                logger.error("Unhandled alert, retrying...");
                handleAlert(driver);
                return null;
            } catch (Exception e) {
                logger.error("Error finding detailed link in card: {}", card.getText(), e);
                return null;
            }
        }
        return null;
    }

    private Patent fetchPatentDetails(String url, String category) throws InterruptedException {
        int attempts = 3;
        String docNumber = url.substring(url.lastIndexOf("=") + 1);
        while (attempts > 0) {
            try {
                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                        .timeout(30_000)
                        .get();

                Patent patent = new Patent();
                patent.setDocNumber(docNumber);
                patent.setCategory(category);
                patent.setPatentSite("gosreestr.kazpatent.kz");

                List<PatentAdditionalField> additionalFields = new ArrayList<>();

                Elements fields = doc.select("div.detial_plan_info ul li"); // Select all list items

                for (Element field : fields) {
                    String rawLabel = field.select("strong").text().trim();
                    String value = field.select("span").text().trim();


                    // Remove context starting with ( and ending )
                    String label = rawLabel.replaceAll("\\(.*\\)", "").trim();

                    if(label.isEmpty() || value.isEmpty()) {
                        continue;
                    }
                    if (label.equals("Реферат/Описание") || label.equals("Описания"))
                        //i need to get absolute url
                        value = field.select("span a").attr("abs:href");

                    if (isCoreField(label)) {
                        setCoreField(patent, label, value);
                    } else {
                        additionalFields.add(new PatentAdditionalField(patent, label, value));
                    }
                }

                Element imgElement = doc.selectFirst("div.plan_img5 img, div.plan_img img");
                if (imgElement != null) {
                    String imgUrl = imgElement.absUrl("src");
                    if(imgUrl != null) {
                        patent.setImageUrl(imgUrl);
                    }
                }

                patent.setAdditionalFields(additionalFields);
                return patent;

            } catch (SocketTimeoutException e) {
                logger.warn("Timeout, retrying...");
                attempts--;
                Thread.sleep(3000);
            } catch (HttpStatusException e) {
                if (e.getStatusCode() == 500) {
                    patentStorageService.saveDocNumber(category, docNumber, false);
                    logger.info("Saving docNumber: {}", docNumber);
                    logger.error("Error fetching patent details because of 500 status");
                    return null;
                } else {
                    logger.error("HTTP error: " + e.getStatusCode());
                }
            } catch (IOException e) {

                patentStorageService.saveDocNumber(category, docNumber, false);
                logger.error("Error fetching patent details: {}", e.getMessage());
                logger.info("Saving docNumber: {}", docNumber);
                return null;
            }
        }
        return null;
    }
    public boolean switchToTextView(WebDriver driver, WebDriverWait wait) {
        try {
            WebElement textViewButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[@data-view='2']")));

            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", textViewButton);
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", textViewButton);

            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("LoadingPanel_LD")));
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("cvReestr_LD")));

            logger.info("Switched to 'Текст' view successfully.");
            return true;
        } catch (TimeoutException | NoSuchElementException e) {
            logger.error("Element not found or not clickable for switching to 'Текст' view: {}", e.getMessage(), e);
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error while switching to 'Текст' view: {}", e.getMessage(), e);
            return false;
        }
    }

    private void setPageSizeTo200(WebDriver webDriver, WebDriverWait wait) {
        try {
            Thread.sleep(3000);
            WebElement pageSizeDropdown = wait.until(ExpectedConditions.elementToBeClickable(By.id("cvReestr_DXPagerTop_PSB")));
            pageSizeDropdown.click();
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("LoadingPanel_LD")));

            Thread.sleep(2000);

            WebElement lastOption = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.id("cvReestr_DXPagerTop_PSP_DXI4_")
            ));
            lastOption.click();

            // Wait for the page to reload after changing page size
            Thread.sleep(2000);
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("LoadingPanel_LD")));
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("cvReestr_LD")));

        } catch (StaleElementReferenceException e) {

        } catch (Exception e) {
            logger.error("Failed to set page size to 200", e);
        }
    }


    private void setCoreField(Patent patent, String label, String value) {
        switch (label) {
            case "№ охранного документа":
                patent.setSecurityDocNumber(value);
                break;
            case "№ регистрации":
                patent.setRegistrationNumber(value);
                break;
            case "Статус":
                patent.setStatus(value);
                break;
            case "Номер заявки":
                patent.setApplicationNumber(value);
                break;
            case "Дата подачи заявки":
                patent.setFilingDate(LocalDate.parse(value, DATE_FORMATTER));
                break;
            case "Дата регистрации":
                patent.setRegistrationDate(LocalDate.parse(value.substring(0, 10), DATE_FORMATTER));
                break;
            case "Срок действия":
                patent.setExpirationDate(LocalDate.parse(value, DATE_FORMATTER));
                break;
            case "Название":
                patent.setTitle(value);
                break;
            case "МПК":
                patent.setIpc(value);
                break;
            case "МКПО":
                patent.setMkpo(value);
                break;
            case "МКТУ":
                patent.setMkpo(value);
                break;
            case "Номер бюллетеня":
                patent.setBulletinNumber(value);
                break;
            case "Дата бюллетеня":
                patent.setBulletinDate(LocalDate.parse(value, DATE_FORMATTER));
                break;
            case "Наименование сорта, породы":
                patent.setSortName(value);
                break;
            case "Патентообладатель":
                patent.setPatentHolder(value);
                break;
            case "Автор(-ы)":
                patent.setAuthors(value);
                break;
            case "Владелец":
                patent.setOwner(value);
                break;
        }
    }


    private Patent extractPatentData(String cardText, String category) {
        Patent patent = new Patent();
        patent.setPatentSite("gosreestr.kazpatent.kz");
        patent.setCategory(category);
        // Extracting common fields
//        patent.setSecurityDocNumber(getFieldValue(cardText, "№ охранного документа:?"));
//        patent.setRegistrationNumber(getFieldValue(cardText, "№ регистрации:?"));
//        patent.setStatus(getFieldValue(cardText, "Статус:?"));
//        patent.setApplicationNumber(getFieldValue(cardText, "Номер заявки:?"));
//        patent.setFilingDate(getDateField(cardText, "Дата подачи заявки:?"));
//        patent.setRegistrationDate(getDateField(cardText, "Дата регистрации:?"));
//        patent.setExpirationDate(getDateField(cardText, "Срок действия:?"));
        String title = fixMixedCharacters(getFieldValue(cardText, "Название:?"));
        patent.setTitle(title);
//        patent.setIpc(getFieldValue(cardText, "МПК:?"));
//        patent.setMkpo(getFieldValue(cardText, "МКПО:?|МКТУ:?"));
        patent.setBulletinNumber(getFieldValue(cardText, "Номер бюллетеня:?"));
        patent.setBulletinDate(getDateField(cardText, "Дата бюллетеня:?"));
        patent.setAuthors(getFieldValue(cardText, "Автор\\(-ы\\)?:?"));
        patent.setSortName(getFieldValue(cardText, "Наименование сорта, породы:?"));
        //if sort name is not empty and title is empty, set title to sort name
        if (patent.getSortName() != null && patent.getTitle() == null) {
            patent.setTitle(patent.getSortName());
        }
//        patent.setPatentHolder(getFieldValue(cardText, "Патентообладатель:?"));
//        patent.setOwner(getFieldValue(cardText, "Владелец:?"));

        return patent;
    }

    private boolean isCoreField(String label) {
        return List.of("Название", "Номер заявки", "Дата подачи заявки", "Дата регистрации", "Срок действия", "Автор(-ы)", "Патентообладатель",
                "№ охранного документа", "№ регистрации", "Статус", "МПК", "МКПО", "МКТУ", "Номер бюллетеня", "Дата бюллетеня", "Владелец", "Наименование сорта, породы").contains(label);
    }

    private String getFieldValue(String text, String regex) {
        Pattern pattern = Pattern.compile(regex + "\s*:?\s*(.*)");
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? (matcher.group(1) != null ? matcher.group(1).trim() : "") : null;
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

    private void handleAlert(WebDriver driver) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(2));
            wait.until(ExpectedConditions.alertIsPresent()); // Ждём alert
            Alert alert = driver.switchTo().alert();
            logger.warn("Alert detected: " + alert.getText());
            alert.accept(); // Закрываем alert
        } catch (NoAlertPresentException e) {}
    }
}
