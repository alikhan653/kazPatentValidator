package kz.it.patentparser.processor;

import kz.it.patentparser.parser.EbulletinPatentParser;
import kz.it.patentparser.parser.GosReestrPatentParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PatentProcessor {
    private static final Logger logger = LoggerFactory.getLogger(PatentProcessor.class);
    private final GosReestrPatentParser gosReestrPatentParser;
    private final EbulletinPatentParser ebulletinPatentParser;

    public PatentProcessor(GosReestrPatentParser gosReestrPatentParser, EbulletinPatentParser ebulletinPatentParser) {
        this.gosReestrPatentParser = gosReestrPatentParser;
        this.ebulletinPatentParser = ebulletinPatentParser;
    }

    /**
     * Запуск всех парсеров для всех категорий
     */
    public void runAllParsers() {
        logger.info("Starting all patent parsers...");

        runAllCategoriesForParser("gosreestr");
        runAllCategoriesForParser("ebulletin");

        logger.info("All patent parsers have finished.");
    }

    /**
     * Запуск всех категорий для одного парсера
     * @param parserName - Название парсера ("gosreestr" или "ebulletin")
     */
    public void runAllCategoriesForParser(String parserName) {
        switch (parserName.toLowerCase()) {
            case "gosreestr":
                logger.info("Starting all categories for GosReestrPatentParser...");
                gosReestrPatentParser.parseAll();
                logger.info("Finished parsing all categories for GosReestr.");
                break;

            case "ebulletin":
                logger.info("Starting all categories for EbulletinPatentParser...");
                ebulletinPatentParser.parseAll();
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
                ebulletinPatentParser.parseCategory(category);
                logger.info("Finished parsing category {} for Ebulletin.", category);
                break;

            default:
                logger.warn("Unknown parser name: {}", parserName);
        }
    }



}
