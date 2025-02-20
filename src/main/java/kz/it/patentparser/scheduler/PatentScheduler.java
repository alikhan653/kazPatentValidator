package kz.it.patentparser.scheduler;

import kz.it.patentparser.processor.PatentProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PatentScheduler {
    private static final Logger logger = LoggerFactory.getLogger(PatentScheduler.class);
    private final PatentProcessor patentProcessor;

    public PatentScheduler(PatentProcessor patentProcessor) {
        this.patentProcessor = patentProcessor;
    }

    // Запуск парсеров каждую неделю в понедельник в 03:00
    @Scheduled(cron = "0 0 3 * * MON")
    public void runParsersWeekly() {
        logger.info("Starting weekly patent parsing...");
        patentProcessor.runAllParsers();
        logger.info("Weekly patent parsing completed.");
    }
}
