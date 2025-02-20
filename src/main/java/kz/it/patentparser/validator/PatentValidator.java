package kz.it.patentparser.validator;

import kz.it.patentparser.model.Patent;
import org.springframework.stereotype.Component;

@Component
public class PatentValidator {

    public boolean isValid(Patent patent) {
//        return isNotEmpty(patent.getTitle()) &&
//               isNotEmpty(patent.getApplicationNumber()) &&
//               isNotEmpty(patent.getFilingDate()) &&
//               isNotEmpty(patent.getAuthors()) &&
//               isNotEmpty(patent.getPatentHolder()) &&
//               isNotEmpty(patent.getSecurityDocNumber()) &&
//               isNotEmpty(patent.getStatus()) &&
//               isNotEmpty(patent.getIpc()) &&
//               isNotEmpty(patent.getBulletinNumber()) &&
//               isNotEmpty(patent.getBulletinDate());
        return isNotEmpty(patent.getStatus());
    }

    private boolean isNotEmpty(String value) {
        return value != null && !value.isEmpty();
    }
}
