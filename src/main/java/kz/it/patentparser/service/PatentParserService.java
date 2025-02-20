package kz.it.patentparser.service;

import kz.it.patentparser.model.Patent;
import kz.it.patentparser.repository.PatentRepository;
import kz.it.patentparser.validator.PatentValidator;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PatentParserService {

    private static final Logger logger = LoggerFactory.getLogger(PatentParserService.class);

    private final PatentRepository patentRepository;

    private final PatentValidator validator;


    public PatentParserService(PatentRepository patentRepository, PatentValidator validator) {
        this.patentRepository = patentRepository;
        this.validator = validator;
    }

    private boolean isPatentExists(Patent patent) {
        Optional<Patent> existingPatent = patentRepository.findByApplicationNumber(patent.getApplicationNumber());
        return existingPatent.isPresent();
    }

}