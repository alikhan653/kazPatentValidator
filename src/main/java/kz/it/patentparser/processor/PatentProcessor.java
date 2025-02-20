package kz.it.patentparser.processor;

import kz.it.patentparser.parser.EbulletinPatentParser;
import kz.it.patentparser.parser.GosReestrPatentParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
                gosReestrPatentParser.parse();
                logger.info("Finished parsing all categories for GosReestr.");
                break;

            case "ebulletin":
                logger.info("Starting all categories for EbulletinPatentParser...");
                ebulletinPatentParser.parse();
                logger.info("Finished parsing all categories for OtherSite.");
                break;

            default:
                logger.warn("Unknown parser name: {}", parserName);
        }
    }
}
