package kz.it.patentparser.controller;


import kz.it.patentparser.parser.PatentParser;
import kz.it.patentparser.processor.PatentProcessor;
import kz.it.patentparser.service.PatentParserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patents")
public class PatentController {

    private final PatentProcessor patentProcessor;


    public PatentController(PatentProcessor patentProcessor, PatentParserService patentParserService) {
        this.patentProcessor = patentProcessor;
    }

    @PostMapping("/parse")
    public void parse() {
        patentProcessor.runAllParsers();
    }
}
