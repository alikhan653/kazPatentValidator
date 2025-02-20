package kz.it.patentparser.parser;

import kz.it.patentparser.model.Patent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EbulletinPatentParser implements PatentParser {
    @Override
    public List<Patent> parse() {
        return null;
    }
}
