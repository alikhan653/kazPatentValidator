package kz.it.patentparser.controller;



import kz.it.patentparser.enums.NavigationDirection;
import kz.it.patentparser.parser.PatentParser;
import kz.it.patentparser.processor.PatentProcessor;
import kz.it.patentparser.service.PatentApiClient;
import kz.it.patentparser.service.PatentService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/patents")
public class PatentController {

    private final PatentProcessor patentProcessor;
    private final PatentService patentService;
    private final PatentApiClient patentApiClient;


    public PatentController(PatentProcessor patentProcessor, PatentService patentService, PatentApiClient patentApiClient) {
        this.patentProcessor = patentProcessor;
        this.patentService = patentService;
        this.patentApiClient = patentApiClient;
    }

    @PostMapping("/parse")
    public String parse() {
        patentProcessor.runAllParsers();
        return "Парсинг завершен!";
    }

    @PostMapping("/parse/{parserName}")
    public String parse(@PathVariable String parserName) {
        patentProcessor.runAllCategoriesForParser(parserName, NavigationDirection.NEXT.getClassName(), false);
        return "Парсинг завершен!";
    }
    @PostMapping("/parse/gosreestr/both")
    public String parse1() {
        patentProcessor.runParserForAllBothFromStartAndEnd("gosreestr");
        return "Парсинг завершен!";
    }

    @PostMapping("/parse/{parserName}/{category}")
    public String parse(@PathVariable String parserName, @PathVariable String category) {
        patentProcessor.runParser(parserName, category);
        return "Парсинг завершен!";
    }
    @PostMapping("/parse/{parserName}/{category}/end")
    public String parse1(@PathVariable String parserName, @PathVariable String category) {
        patentProcessor.runParserFrom(parserName, category, NavigationDirection.PREVIOUS.getClassName(), false);
        return "Парсинг завершен!";
    }
    @PostMapping("/parse/{parserName}/{category}/start")
    public String parse2(@PathVariable String parserName, @PathVariable String category) {
        patentProcessor.runParserFrom(parserName, category, NavigationDirection.NEXT.getClassName(), false);
        return "Парсинг завершен!";
    }
    @PostMapping("/parse/gosreestr/{category}/both")
    public String parse3(@PathVariable String category) {
        patentProcessor.runParserBothFromStartAndEnd(category);
        return "Парсинг завершен!";
    }

//    @PostMapping("/check/{category}/{from}/{to}")
//    public String check(@PathVariable String category, @PathVariable int from, @PathVariable int to) {
//        patentCheckerService.startProcessing(category, from, to);
//        return "Проверка завершена!";
//    }

    @PostMapping("/retry/image/{order}")
    public String retry1(@PathVariable String order) {
        patentProcessor.runImageScraper(order);
        return "Повторная проверка завершена!";
    }
    @PostMapping("/retry/failed")
    public String retry2() {
        patentProcessor.runRetryService();
        return "Повторная проверка завершена!";
    }

    @GetMapping("/image/ebulletin/{patentId}/{endpoint}")
    public Mono<String> fetchImage(@PathVariable String patentId, @PathVariable String endpoint) {
        return patentApiClient.fetchImageBase64(patentId, endpoint);
    }
}
