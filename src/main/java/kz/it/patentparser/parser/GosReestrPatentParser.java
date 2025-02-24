package kz.it.patentparser.parser;

import kz.it.patentparser.model.Patent;
import kz.it.patentparser.model.PatentAdditionalField;
import kz.it.patentparser.repository.DocNumberRepository;
import kz.it.patentparser.service.PatentService;
import kz.it.patentparser.service.PatentStorageService;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GosReestrPatentParser implements PatentParser {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final Logger logger = LoggerFactory.getLogger(GosReestrPatentParser.class);

    private final PatentValidator validator;
    private final PatentService patentService;

    private final PatentStorageService patentStorageService;

    @Autowired
    public GosReestrPatentParser(PatentService patentService, PatentValidator validator, PatentStorageService patentStorageService) {
        this.patentService = patentService;
        this.validator = validator;
        this.patentStorageService = patentStorageService;
    }

    @Override
    public List<Patent> parseAll() {
        List<Patent> patents = new ArrayList<>();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--no-sandbox");
        WebDriver webDriver = new ChromeDriver(options);

        try {
            WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(20));

            logger.info("Starting patent parsing process...");
            webDriver.get("https://gosreestr.kazpatent.kz/");
            logger.info("Opened KazPatent website.");

            Map<String, String> categories = getCategories();

            for (Map.Entry<String, String> category : categories.entrySet()) {
                try {
                    logger.info("Processing category: {}", category.getKey());
                    selectCategory(webDriver, wait, category.getKey(), category.getValue());
                    patents.addAll(parsePatentsWithPagination(webDriver, wait, category.getKey()));
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
        List<Patent> patents = new ArrayList<>();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--no-sandbox");
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
                selectCategory(webDriver, wait, category, categoryId);
                patents.addAll(parsePatentsWithPagination(webDriver, wait, category));
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

    private List<Patent> parsePatentsWithPagination(WebDriver webDriver, WebDriverWait wait, String category) throws InterruptedException {
        List<Patent> patents = new ArrayList<>();
        int currentPage = 1;

        setPageSizeTo200(webDriver, wait);
        Thread.sleep(3000);
        JavascriptExecutor js = (JavascriptExecutor) webDriver;

        while (true) {
            scroll(js, false);
            List<Patent> pagePatents = parsePatents(webDriver, wait, category, js);

            savePatentData(pagePatents);

            patents.addAll(pagePatents);
            logger.info("Parsed {} patents on page: {}", pagePatents.size(), currentPage);
            try {
                // Wait until all buttons are visible
                List<WebElement> pageButtons = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(
                        By.cssSelector("a.dxp-button.dxp-bi.dxRoundRippleTarget.dxRippleTarget")));
                wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("cvReestr_LD")));

                Optional<WebElement> nextButton = pageButtons.stream()
                        .filter(button -> {
                            List<WebElement> images = button.findElements(By.tagName("img"));
                            return images.stream().anyMatch(img -> img.getAttribute("class").contains("dxWeb_pNext_Material"));
                        })
                        .findFirst();

                if (nextButton.isPresent()) {
                    nextButton.get().click();
                    wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("LoadingPanel_LD")));
                    logger.info("Navigated to page: {}", ++currentPage);
                } else {
                    logger.info("No 'Next' button found. Stopping pagination.");
                    break;
                }
            } catch (NoSuchElementException | TimeoutException e) {
                logger.info("Pagination stopped. No more pages or timeout: " + e.getMessage());
                break;
            }

        }
        return patents;
    }

    private List<Patent> parsePatents(WebDriver webDriver, WebDriverWait wait, String category, JavascriptExecutor js) throws InterruptedException {
        List<Patent> patents = new ArrayList<>();
        logger.debug("Parsing patents for category: {}", category);

        List<WebElement> patentCards = new ArrayList<>();
        try{
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("cvReestr_DXMainTable")));
            logger.debug("Search results loaded for category: {}", category);

            scroll(js, true);

            patentCards = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(
                    By.cssSelector("div.dxcvFlowCard_Material")));
        } catch (TimeoutException e) {
            logger.error("Timeout while waiting for patent cards to load: {}", e.getMessage());
        } catch (NoSuchElementException e) {
            logger.error("No patent cards found for category: {}", category);
        } catch (StaleElementReferenceException e) {
            logger.error("Stale element reference while waiting for patent cards to load: {}", e.getMessage());
        }

        if (patentCards.isEmpty()) {
            logger.warn("No patents found for category: {}", category);
            return patents;
        }

        for (WebElement card : patentCards) {
            try {
                WebElement linkElement = retryFindElement(card, js);
                String detailUrl = linkElement.getAttribute("href");

                if (detailUrl == null || detailUrl.isEmpty()) {
                    logger.warn("No detailed link found for a patent.");
                    continue;
                }
                Patent patent = extractPatentData(card.getText(), category);
                logger.debug("Extracting patent data from card: {}", card.getText());

                logger.debug("Fetching detailed patent page: {}", detailUrl);
                Patent detailedPatent = fetchPatentDetails(detailUrl, category);

                mergePatentData(patent, detailedPatent);

                if (patentService.isPatentExists(patent)) {
                    assert patent != null;
                    logger.info("Patent already exists, skipping: {}", patent.getSecurityDocNumber()!=null?patent.getSecurityDocNumber():patent.getRegistrationNumber());
                    continue;
                }

                if (validator.isValid(patent)) {
                    patents.add(patent);
                    logger.info("Saved patent: {}", patent.getSecurityDocNumber()!=null?patent.getSecurityDocNumber():patent.getRegistrationNumber());
                } else {
                    logger.warn("Invalid patent data, skipping: {}", patent);
                }
            } catch (NoSuchElementException e) {
                logger.error("Error extracting patent data for category: {}", category, e);
            } catch (StaleElementReferenceException e) {
                logger.error("Stale element reference while parsing patent, showing card data and error: {}, {}", card.getText(), e.getMessage());
            }
        }
        return patents;
    }

    private void savePatentData(List<Patent> patents) {
        logger.info("Saving patents to database...");
        patentService.savePatents(patents);
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

    private static void scroll(JavascriptExecutor js, boolean scrollToBottom) throws InterruptedException {
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
    }

    private void mergePatentData(Patent patent, Patent detailedPatent) {
        if (detailedPatent.getTitle() != null) {
            patent.setTitle(detailedPatent.getTitle());
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
        if (detailedPatent.getAdditionalFields() != null) {
            for (PatentAdditionalField field : detailedPatent.getAdditionalFields()) {
                field.setPatent(patent); // Важно!
            }
            patent.setAdditionalFields(detailedPatent.getAdditionalFields());
        }
    }

    private WebElement retryFindElement(WebElement card, JavascriptExecutor js) throws InterruptedException {
        int attempts = 3;
        while (attempts > 0) {
            try {
                return card.findElement(By.cssSelector("a"));
            } catch (StaleElementReferenceException e) {
                attempts--;
                logger.error("Stale element reference finding detailed link, retrying...");
                scroll(js, false);
                Thread.sleep(3000);
                scroll(js, true);
                Thread.sleep(3000);
            } catch (NoSuchElementException e) {
                logger.error("No detailed link found in card: {}", card.getText());
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
        while (attempts > 0) {
            try {
                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                        .timeout(30_000)
                        .get();

                Patent patent = new Patent();
                patent.setCategory(category);

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
                    additionalFields.add(new PatentAdditionalField(patent, "Изображение", imgUrl));
                }

                patent.setAdditionalFields(additionalFields);
                return patent;

            } catch (SocketTimeoutException e) {
                logger.warn("Timeout, retrying...");
                attempts--;
                Thread.sleep(3000);
            } catch (HttpStatusException e) {
                if (e.getStatusCode() == 500) {
                    int docNumber = Integer.parseInt(url.substring(url.lastIndexOf("=") + 1));
                    patentStorageService.saveDocNumber(category, docNumber);
                    logger.info("Saving docNumber: {}", docNumber);
                    logger.error("Error fetching patent details because of 500 status");
                    return null;
                } else {
                    logger.error("HTTP error: " + e.getStatusCode());
                }
            } catch (IOException e) {
                int docNumber = Integer.parseInt(url.substring(url.lastIndexOf("=") + 1));
                patentStorageService.saveDocNumber(category, docNumber);
                logger.error("Error fetching patent details: {}", e.getMessage());
                logger.info("Saving docNumber: {}", docNumber);
                return null;
            }
        }
        return null;
    }

    private void setPageSizeTo200(WebDriver webDriver, WebDriverWait wait) {
        try {
            // Wait for the page size dropdown to be visible
            WebElement pageSizeDropdown = wait.until(ExpectedConditions.elementToBeClickable(By.id("cvReestr_DXPagerTop_PSB")));
            pageSizeDropdown.click();

            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("LoadingPanel_LD")));

            WebElement lastOption = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.id("cvReestr_DXPagerTop_PSP_DXI4_")
            ));
            lastOption.click();

            // Wait for the page to reload after changing page size
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("LoadingPanel_LD")));
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("cvReestr_LD")));

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
                patent.setRegistrationDate(LocalDate.parse(value, DATE_FORMATTER));
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
        patent.setSecurityDocNumber(getFieldValue(cardText, "№ охранного документа:?"));
        patent.setRegistrationNumber(getFieldValue(cardText, "№ регистрации:?"));
        patent.setStatus(getFieldValue(cardText, "Статус:?"));
        patent.setApplicationNumber(getFieldValue(cardText, "Номер заявки:?"));
        patent.setFilingDate(getDateField(cardText, "Дата подачи заявки:?"));
        patent.setRegistrationDate(getDateField(cardText, "Дата регистрации:?"));
        patent.setExpirationDate(getDateField(cardText, "Срок действия:?"));
        patent.setTitle(getFieldValue(cardText, "Название:?|Наименование сорта, породы:?"));
        patent.setIpc(getFieldValue(cardText, "МПК:?"));
        patent.setMkpo(getFieldValue(cardText, "МКПО:?"));
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
        return List.of("Название", "Номер заявки", "Дата подачи заявки", "Дата регистрации", "Срок действия", "Автор(-ы)", "Патентообладатель",
                "№ охранного документа", "№ регистрации", "Статус", "МПК", "МКПО", "Номер бюллетеня", "Дата бюллетеня", "Владелец", "Наименование сорта, породы").contains(label);
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
}
