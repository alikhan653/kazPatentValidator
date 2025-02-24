package kz.it.patentparser.validator;

import kz.it.patentparser.model.Patent;
import org.springframework.stereotype.Component;

@Component
public class PatentValidator {

    public boolean isValid(Patent patent) {
        try{
            return isNotEmpty(patent.getStatus());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isNotEmpty(String value) {
        return value != null && !value.isEmpty();
    }
}
