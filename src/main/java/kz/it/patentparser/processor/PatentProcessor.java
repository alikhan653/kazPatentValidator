package kz.it.patentparser.processor;

import kz.it.patentparser.enums.NavigationDirection;
import kz.it.patentparser.parser.EbulletinPatentFetcher;
import kz.it.patentparser.parser.EbulletinPatentParser;
import kz.it.patentparser.parser.GosReestrPatentParser;
import kz.it.patentparser.service.PatentRetryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PatentProcessor {
    private static final Logger logger = LoggerFactory.getLogger(PatentProcessor.class);
    private final GosReestrPatentParser gosReestrPatentParser;
    private final EbulletinPatentParser ebulletinPatentParser;
    private final EbulletinPatentFetcher ebulletinPatentFetcher;
    private final PatentRetryService patentRetryService;

    public PatentProcessor(GosReestrPatentParser gosReestrPatentParser, EbulletinPatentParser ebulletinPatentParser, EbulletinPatentFetcher ebulletinPatentFetcher, PatentRetryService patentRetryService) {
        this.gosReestrPatentParser = gosReestrPatentParser;
        this.ebulletinPatentParser = ebulletinPatentParser;
        this.ebulletinPatentFetcher = ebulletinPatentFetcher;
        this.patentRetryService = patentRetryService;
    }

    /**
     * Запуск всех парсеров для всех категорий
     */
    public void runAllParsers() {
        logger.info("Starting all patent parsers...");

        runParserForAllBothFromStartAndEnd("gosreestr");
//        runAllCategoriesForParser("ebulletin");
        runAllCategoriesForParser("ebulletin", "", false);

        logger.info("All patent parsers have finished.");
    }

    /**
     * Запуск всех категорий для одного парсера
     * @param parserName - Название парсера ("gosreestr" или "ebulletin")
     */
    public void runAllCategoriesForParser(String parserName, String from, boolean both) {
        switch (parserName.toLowerCase()) {
            case "gosreestr":
                logger.info("Starting all categories for GosReestrPatentParser...");
                gosReestrPatentParser.parseAll(from, both);
                logger.info("Finished parsing all categories for GosReestr.");
                break;

            case "ebulletin":
                logger.info("Starting all categories for EbulletinPatentParser...");
                ebulletinPatentFetcher.parseAll(from, both);
                logger.info("Finished parsing all categories for Ebulletin.");
                break;

            default:
                logger.warn("Unknown parser name: {}", parserName);
        }
    }

    /**
     * Запуск парсера для одной категории
     * @param parserName - Название парсера ("gosreestr" или "ebulletin")
     * @param category - Название категории
     */
    public void runParser(String parserName, String category) {
        switch (parserName.toLowerCase()) {
            case "gosreestr":
                logger.info("Starting parsing category {} for GosReestrPatentParser...", category);
                gosReestrPatentParser.parseCategory(category);
                logger.info("Finished parsing category {} for GosReestr.", category);
                break;

            case "ebulletin":
                logger.info("Starting parsing category {} for EbulletinPatentParser...", category);
                ebulletinPatentFetcher.parseCategory(category);
                logger.info("Finished parsing category {} for Ebulletin.", category);
                break;

            default:
                logger.warn("Unknown parser name: {}", parserName);
        }
    }

    public void runParserFrom(String parserName, String category, String from, boolean both) {
        switch (parserName.toLowerCase()) {
            case "gosreestr":
                logger.info("Starting parsing category {} for GosReestrPatentParser...", category);
                gosReestrPatentParser.parseFrom(category, from, both);
                logger.info("Finished parsing category {} for GosReestr.", category);
                break;

            case "ebulletin":
                logger.info("Starting parsing category {} for EbulletinPatentParser...", category);
                ebulletinPatentFetcher.parseFrom(category, from, both);
                logger.info("Finished parsing category {} for Ebulletin.", category);
                break;

            default:
                logger.warn("Unknown parser name: {}", parserName);
        }
    }

    public void runParserBothFromStartAndEnd(String category) {
        ExecutorService executor = Executors.newFixedThreadPool(2); // Two parallel tasks
        logger.info("Starting parsing category {} for GosReestrPatentParser...", category);
        Future<?> futureNext = executor.submit(() -> runParserFrom("gosreestr", category, NavigationDirection.NEXT.getClassName(), true));
        Future<?> futurePrevious = executor.submit(() -> runParserFrom("gosreestr", category, NavigationDirection.PREVIOUS.getClassName(), true));


        try {
            futureNext.get(); // Wait for start-to-end parsing to finish
            futurePrevious.get(); // Wait for end-to-start parsing to finish
        } catch (Exception e) {
            logger.error("Error executing parsing tasks", e);
        } finally {
            executor.shutdown(); // Shut down executor
        }
    }

    public void runParserForAllBothFromStartAndEnd(String parserName) {
        ExecutorService executor = Executors.newFixedThreadPool(2); // Two parallel tasks
        logger.info("Starting parsing all category for GosReestrPatentParser...");
        Future<?> futureNext = executor.submit(() -> runAllCategoriesForParser(parserName, NavigationDirection.NEXT.getClassName(), true));
        Future<?> futurePrevious = executor.submit(() -> runAllCategoriesForParser(parserName, NavigationDirection.PREVIOUS.getClassName(), true));

        try {
            futureNext.get(); // Wait for start-to-end parsing to finish
            futurePrevious.get(); // Wait for end-to-start parsing to finish
        } catch (Exception e) {
            logger.error("Error executing parsing tasks", e);
        } finally {
            executor.shutdown(); // Shut down executor
        }
    }

    public void runRetryService() {
        patentRetryService.retryFailedPatents();
    }

    public void runImageScraper(String order) {
        patentRetryService.fetchMissingImages(order);
    }

}
