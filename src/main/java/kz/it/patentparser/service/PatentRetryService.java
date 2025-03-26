package kz.it.patentparser.service;

import kz.it.patentparser.model.DocNumber;
import kz.it.patentparser.model.Patent;
import kz.it.patentparser.model.PatentAdditionalField;
import kz.it.patentparser.repository.DocNumberRepository;
import kz.it.patentparser.util.ImageScraper;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class PatentRetryService {
    private static final Logger logger = LoggerFactory.getLogger(PatentRetryService.class);
    private final PatentStorageService patentStorageService;
    private final PatentService patentService;
    private final DocNumberRepository failedPatentRepository;

    public PatentRetryService(PatentStorageService patentStorageService, DocNumberRepository failedPatentRepository, PatentService patentService) {
        this.patentStorageService = patentStorageService;
        this.failedPatentRepository = failedPatentRepository;
        this.patentService = patentService;
    }

//    @Scheduled(fixedRate = 86400000) // Run once per day
    public void retryFailedPatents() {
        logger.info("Starting retry for all failed patents");
        List<DocNumber> failedPatents = failedPatentRepository.findByIsParsedFalse();

        for (DocNumber failedPatent : failedPatents) {
            String url = generatePatentUrl(Integer.parseInt(failedPatent.getDocumentNumber()), failedPatent.getCategory());
            try {
                Patent patent = fetchPatentDetails(url, failedPatent.getCategory());
                if (patent != null) {
                    if(patentService.isPatentExists(patent)) {
                        failedPatent.setParsed(true);
                        failedPatentRepository.save(failedPatent);
                    } else {
                        patentService.savePatent(patent);
                        failedPatent.setParsed(true);
                        failedPatentRepository.save(failedPatent);
                    }
                }
                logger.info("Retry successful for docNumber: {}", failedPatent.getDocumentNumber());
            } catch (Exception e) {
                logger.error("Retry failed for docNumber: {}", failedPatent.getDocumentNumber(), e);
            }
        }
        logger.info("Retry finished for all failed patents");
    }

//    @Scheduled(fixedRate = 86400000) // Run once per day
public void fetchMissingImages() {
    logger.info("Starting parallel image fetching for patents without images");

    List<DocNumber> patentsWithoutImages = failedPatentRepository.findPatentsWithoutImages();
    ExecutorService executor = Executors.newFixedThreadPool(10); // Adjust pool size as needed

    List<CompletableFuture<Void>> futures = patentsWithoutImages.stream()
            .map(patent -> CompletableFuture.runAsync(() -> processPatentImage(patent), executor))
            .collect(Collectors.toList());

    // Wait for all tasks to complete
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    executor.shutdown();
    logger.info("Image fetching finished for patents without images");
}

    private void processPatentImage(DocNumber patent) {
        String url = generatePatentUrl(Integer.parseInt(patent.getDocumentNumber()), patent.getCategory());
        try {
            String imageBase64 = ImageScraper.captureImageBase64(url, logger);
            if (imageBase64 != null) {
                Patent patentEntity = patentService.getPatentByDocNumber(patent.getDocumentNumber());
                patentService.saveAdditionalField(patentEntity.getId(), "imageBase64", imageBase64);
                logger.info("Image fetched for docNumber: {}", patent.getDocumentNumber());
            } else {
                logger.warn("No image found for docNumber: {}", patent.getDocumentNumber());
            }
        } catch (Exception e) {
            logger.error("Error fetching image for docNumber: {}", patent.getDocumentNumber(), e);
        }
    }

    private String getCategory(String category) {
        switch (category) {
            case "Товарные знаки":
                return "Trademark";
            case "Изобретения":
                return "Invention";
            case "Полезные модели":
                return "UtilityModel";
            case "Промышленные образцы":
                return "IndustrialDesign";
            case "Селекционные достижения":
                return "SelectionAchievement";
            default:
                return "";
        }
    }

    private String generatePatentUrl(int documentNumber, String category) {
        return "https://gosreestr.kazpatent.kz/" + getCategory(category) + "/Details?docNumber=" + documentNumber;
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
                patent.setPatentSite("gosreestr.kazpatent.kz");

                List<PatentAdditionalField> additionalFields = new ArrayList<>();
                Elements fields = doc.select("div.detial_plan_info ul li");

                for (Element field : fields) {
                    String rawLabel = field.select("strong").text().trim();
                    String value = field.select("span").text().trim();
                    String label = rawLabel.replaceAll("\\(.*\\)", "").trim();
                    if (label.isEmpty() || value.isEmpty()) continue;
                    if (label.equals("Реферат/Описание") || label.equals("Описания")) {
                        value = field.select("span a").attr("abs:href");
                    }
                    additionalFields.add(new PatentAdditionalField(patent, label, value));
                }

                patent.setAdditionalFields(additionalFields);
                return patent;

            } catch (SocketTimeoutException e) {
                logger.warn("Timeout, retrying...");
                attempts--;
                Thread.sleep(3000);
            } catch (HttpStatusException e) {
                if (e.getStatusCode() == 500) {
                    return null;
                }
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }
}
