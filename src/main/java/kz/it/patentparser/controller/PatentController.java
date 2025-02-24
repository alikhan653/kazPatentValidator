package kz.it.patentparser.controller;


import kz.it.patentparser.parser.PatentParser;
import kz.it.patentparser.processor.PatentProcessor;
import kz.it.patentparser.service.PatentCheckerService;
import kz.it.patentparser.service.PatentDataService;
import kz.it.patentparser.service.PatentParserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patents")
public class PatentController {

    private final PatentProcessor patentProcessor;
    private final PatentCheckerService patentCheckerService;
    private final PatentDataService patentDataService;



    public PatentController(PatentProcessor patentProcessor, PatentDataService patentDataService, PatentCheckerService patentCheckerService) {
        this.patentProcessor = patentProcessor;
        this.patentCheckerService = patentCheckerService;
        this.patentDataService = patentDataService;
    }

    @PostMapping("/parse")
    public String parse() {
        patentProcessor.runAllParsers();
        return "Парсинг завершен!";
    }

    @PostMapping("/parse/{parserName}")
    public String parse(@PathVariable String parserName) {
        patentProcessor.runAllCategoriesForParser(parserName);
        return "Парсинг завершен!";
    }

    @PostMapping("/parse/{parserName}/{category}")
    public String parse(@PathVariable String parserName, @PathVariable String category) {
        patentProcessor.runParser(parserName, category);
        return "Парсинг завершен!";
    }

    @PostMapping("/check/{category}/{from}/{to}")
    public String check(@PathVariable String category, @PathVariable int from, @PathVariable int to) {
        patentCheckerService.startProcessing(category, from, to);
        return "Проверка завершена!";
    }


    @GetMapping("/sendApi")
    public String sendApi() {
        return patentDataService.getIZPatentData();
    }
}
