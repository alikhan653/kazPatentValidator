package kz.it.patentparser.parser;

import kz.it.patentparser.model.Patent;
import kz.it.patentparser.model.PatentAdditionalField;
import kz.it.patentparser.repository.PatentAdditionalFieldRepository;
import kz.it.patentparser.repository.PatentRepository;
import kz.it.patentparser.service.PatentParserService;
import kz.it.patentparser.service.PatentService;
import kz.it.patentparser.validator.PatentValidator;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface PatentParser {
    List<Patent> parseAll();
    List<Patent> parseCategory(String category);
}
