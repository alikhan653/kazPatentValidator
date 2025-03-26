package kz.it.patentparser.util;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import kz.it.patentparser.enums.PatentCategory;
import kz.it.patentparser.model.Patent;
import kz.it.patentparser.model.PatentAdditionalField;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

import static kz.it.patentparser.util.TransliterationUtil.fixMixedCharacters;

public class PatentSpecifications {

    public static Specification<Patent> hasTitle(String query) {
        return (root, query1, cb) -> {
            if (query == null || query.trim().isEmpty()) {
                return cb.conjunction(); // No filtering if query is empty
            }

            String transliteratedQuery1 = "";
            String transliteratedQuery2 = "";
            String cleanedQuery = fixMixedCharacters(query);

            if (TransliterationUtil.isLatin(query)) {
                transliteratedQuery1 = TransliterationUtil.transliterateLatinToCyrillic(cleanedQuery);
                transliteratedQuery1 = TransliterationUtil.transliterateKazakhToRussian(transliteratedQuery1);
                transliteratedQuery2 = transliteratedQuery1;
            } else {
                transliteratedQuery2 = TransliterationUtil.transliterateCyrillicToLatin(cleanedQuery);
                transliteratedQuery1 = transliteratedQuery2;
            }

            return cb.or(
                    cb.like(cb.lower(root.get("title")), "%" + cleanedQuery.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("title")), "%" + transliteratedQuery1.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("title")), "%" + transliteratedQuery2.toLowerCase() + "%")
            );
        };
    }


    public static Specification<Patent> hasExpirationDateBetween(LocalDate start, LocalDate end) {
        //check for null values
        if(start == null) {
            start = LocalDate.of(1700, 1, 1);
        }
        if(end == null) {
            end = LocalDate.of(2100, 1, 1);
        }

        LocalDate finalStart = start;
        LocalDate finalEnd = end;
        return (root, query, cb) -> cb.or(
                cb.between(root.get("expirationDate"), finalStart, finalEnd),  // Date is within range
                cb.isNull(root.get("expirationDate")) // OR expirationDate is NULL
        );
    }

    public static Specification<Patent> hasSiteType(String siteType) {
        return (root, query1, cb) -> cb.equal(root.get("patentSite"), siteType);
    }

    public static Specification<Patent> hasCategory(String category) {
        category = PatentCategory.fromId(Integer.parseInt(category)).getName();
        String finalCategory = category;
        return (root, query1, cb) -> cb.equal(root.get("category"), finalCategory);
    }

    public static Specification<Patent> hasMktu(String mktu) {
        return (root, query, cb) -> {
            // Perform LEFT JOIN on additionalFields
            Join<Patent, PatentAdditionalField> additionalField = root.join("additionalFields", JoinType.LEFT);

            // Filter by the label "Класс МКТУ"
            return cb.and(
                    cb.equal(additionalField.get("label"), "Класс МКТУ"), // Ensure label matches
                    cb.like(cb.lower(additionalField.get("value")), "%" + mktu.toLowerCase() + "%") // Apply search
            );
        };
    }

    public static Specification<Patent> hasSecurityDocNumber(String securityDocNumber) {
        return (root, query1, cb) -> cb.or(
                cb.equal(root.get("securityDocNumber"), securityDocNumber),
                cb.equal(root.get("registrationNumber"), securityDocNumber)
        );
    }

    public static Specification<Patent> isExpired(Boolean expired, LocalDate todayMinus10Years) {
        return (root, query1, cb) -> {
            if (expired == null) {
                return cb.conjunction();
            }
            if (expired) {
                return cb.lessThanOrEqualTo(root.get("registrationDate"), todayMinus10Years);
            } else {
                return cb.greaterThan(root.get("registrationDate"), todayMinus10Years);
            }
        };
    }

    public static Specification<Patent> customSorting() {
        return (root, query, cb) -> {
            query.orderBy(
                    cb.asc(cb.selectCase()
                            .when(root.get("expirationDate").isNull(), 1)
                            .otherwise(0)), // NULL values last
                    cb.desc(root.get("expirationDate")),
                    cb.desc(root.get("id"))
            );
            return cb.conjunction(); // No filtering, only sorting
        };
    }

}