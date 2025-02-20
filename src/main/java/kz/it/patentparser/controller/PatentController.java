package kz.it.patentparser.controller;


import kz.it.patentparser.service.PatentParserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patents")
public class PatentController {

    private final PatentParserService patentParserService;

    public PatentController(PatentParserService patentParserService) {
        this.patentParserService = patentParserService;
    }

    @PostMapping("/parse")
    public String startParsing() {
        patentParserService.parseAllCategories();
        return "Парсинг завершен!";
    }
}
