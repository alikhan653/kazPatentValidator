package kz.it.patentparser.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import kz.it.patentparser.enums.PatentCategory;
import kz.it.patentparser.model.Patent;
import kz.it.patentparser.model.PatentAdditionalField;
import kz.it.patentparser.parser.GosReestrPatentParser;
import kz.it.patentparser.repository.PatentAdditionalFieldRepository;
import kz.it.patentparser.repository.PatentRepository;
import kz.it.patentparser.util.TransliterationUtil;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

import static kz.it.patentparser.util.TransliterationUtil.fixMixedCharacters;

@Service
public class PatentService {
    Logger logger = org.slf4j.LoggerFactory.getLogger(GosReestrPatentParser.class);
    private final PatentRepository patentRepository;
    private final PatentAdditionalFieldRepository additionalFieldRepository;

    @Autowired
    public PatentService(PatentRepository patentRepository, PatentAdditionalFieldRepository additionalFieldRepository) {
        this.patentRepository = patentRepository;
        this.additionalFieldRepository = additionalFieldRepository;
    }

    public Page<Patent> getPatents(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        return patentRepository.findAll(pageable);
    }

    public Optional<Patent> findById(Long id) {
        return patentRepository.findById(id);
    }

    public Page<Patent> searchPatents(String query, LocalDate startDate, LocalDate endDate, int page, int size, String siteType, Boolean expired, String category, String mktu, String securityDocNumber) {
        Pageable pageable = PageRequest.of(page - 1, size);

        if (startDate == null) startDate = LocalDate.of(1800, 1, 1);
        if (endDate == null) endDate = LocalDate.of(2100, 1, 1);
        if (siteType == "") siteType = null;
        System.out.println("startDate = " + startDate + ", endDate = " + endDate);
        String transliteratedQuery1 = "";
        String transliteratedQuery2 = "";
        String cleanedQuery = fixMixedCharacters(query);

        LocalDate todayMinus10Years = LocalDate.now().minusYears(10);  // Текущая дата минус 10 лет

        if (TransliterationUtil.isLatin(query)) {
            transliteratedQuery1 = TransliterationUtil.transliterateLatinToCyrillic(cleanedQuery);
            transliteratedQuery1 = TransliterationUtil.transliterateKazakhToRussian(transliteratedQuery1);
            transliteratedQuery2 = transliteratedQuery1;
        } else {
            transliteratedQuery2 = TransliterationUtil.transliterateCyrillicToLatin(cleanedQuery);
            transliteratedQuery1 = transliteratedQuery2;
        }

        if (category == null || category.isEmpty() || category.equals("0")) {
            category = null;
        } else {
            category = getPatentsByCategory(Integer.parseInt(category));
        }
        logger.info("Searching patents by query: " + query + ", transliteratedQuery1: " + transliteratedQuery1 + ", transliteratedQuery2: " + transliteratedQuery2 + ", startDate: " + startDate + ", endDate: " + endDate + ", siteType: " + siteType + ", expired: " + expired + ", category: " + category + ", mktu: " + mktu + ", securityDocNumber: " + securityDocNumber);
        return patentRepository.searchPatents(query, transliteratedQuery1, transliteratedQuery2, startDate, endDate, siteType, expired, todayMinus10Years, category, mktu, securityDocNumber, pageable);
    }

    public void exportToCsv(HttpServletResponse response, List<Patent> patents) throws IOException {
        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=patents.csv");

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8), true)) {
            // Write BOM to ensure proper encoding in Excel
            writer.write('\uFEFF');

            // Define standard fields (order matters)
            List<String> standardFields = Arrays.asList(
                    "№ Документа", "№ Регистрации", "Статус", "№ Заявки", "Дата подачи",
                    "Дата регистрации", "Дата окончания", "Дата бюллетеня", "Название",
                    "Имя", "№ Бюллетеня", "IPC", "МКПО", "Название сорта",
                    "Патентообладатель", "Авторы", "Владелец", "Категория", "Сайт патента"
            );

            // Get all unique additional field labels
            Set<String> additionalFieldLabels = new HashSet<>();
            for (Patent patent : patents) {
                if (patent.getAdditionalFields() != null) {
                    for (PatentAdditionalField field : patent.getAdditionalFields()) {
                        additionalFieldLabels.add(field.getLabel());
                    }
                }
            }

            // Convert to sorted list for consistent column order
            List<String> additionalFieldHeaders = new ArrayList<>(additionalFieldLabels);
            Collections.sort(additionalFieldHeaders);

            // Write CSV header
            writer.print(String.join(";", standardFields)); // Standard fields first
            for (String header : additionalFieldHeaders) {
                writer.print(";" + header); // Additional dynamic fields
            }
            writer.println();

            // Write each patent's data
            for (Patent patent : patents) {
                List<String> rowData = new ArrayList<>();

                // Standard fields (only if they exist)
                rowData.add(optionalValue(patent.getSecurityDocNumber()));
                rowData.add(optionalValue(patent.getRegistrationNumber()));
                rowData.add(optionalValue(patent.getStatus()));
                rowData.add(optionalValue(patent.getApplicationNumber()));
                rowData.add(optionalDate(patent.getFilingDate()));
                rowData.add(optionalDate(patent.getRegistrationDate()));
                rowData.add(optionalDate(patent.getExpirationDate()));
                rowData.add(optionalDate(patent.getBulletinDate()));
                rowData.add(optionalValue(patent.getTitle()));
                rowData.add(optionalValue(patent.getName()));
                rowData.add(optionalValue(patent.getBulletinNumber()));
                rowData.add(optionalValue(patent.getIpc()));
                rowData.add(optionalValue(patent.getMkpo()));
                rowData.add(optionalValue(patent.getSortName()));
                rowData.add(optionalValue(patent.getPatentHolder()));
                rowData.add(optionalValue(patent.getAuthors()));
                rowData.add(optionalValue(patent.getOwner()));
                rowData.add(optionalValue(patent.getCategory()));
                rowData.add(optionalValue(patent.getPatentSite()));

                // Additional fields dynamically
                Map<String, String> additionalFieldsMap = new HashMap<>();
                if (patent.getAdditionalFields() != null) {
                    for (PatentAdditionalField field : patent.getAdditionalFields()) {
                        additionalFieldsMap.put(field.getLabel(), field.getValue());
                    }
                }

                for (String header : additionalFieldHeaders) {
                    rowData.add(optionalValue(additionalFieldsMap.get(header)));
                }

                // Write row
                writer.println(String.join(";", rowData));
            }
        }
    }


    private String optionalValue(String value) {
        return value != null ? value : "";
    }

    private String optionalDate(LocalDate date) {
        return date != null ? date.toString() : "";
    }


    public String getPatentsByCategory(int categoryId) {
        PatentCategory category = PatentCategory.fromId(categoryId);
        if (category == null) {
            System.out.println("Invalid category ID: " + categoryId);
            throw new IllegalArgumentException("Invalid category ID: " + categoryId);
        }
        return category.getName();
    }

    private String getImageUrl(Patent patent) {
        return patent.getAdditionalFields().stream()
                .filter(field -> "Изображение".equals(field.getLabel()))
                .map(PatentAdditionalField::getValue)
                .findFirst()
                .orElse(null);
    }

    @Transactional
    public Patent savePatent(Patent patent) {
        return patentRepository.save(patent);
    }

    @Transactional
    public void savePatents(List<Patent> patents, Logger logger) {
        patentRepository.saveAll(patents);
    }

    public void saveAdditionalField(Long patentId, String label, String value) {
        Optional<Patent> patentOpt = patentRepository.findById(patentId);

        if (patentOpt.isPresent()) {
            Patent patent = patentOpt.get();
            PatentAdditionalField additionalField = new PatentAdditionalField(patent, label, value);
            additionalFieldRepository.save(additionalField);
        } else {
            throw new EntityNotFoundException("Patent with ID " + patentId + " not found.");
        }
    }

    @Transactional
    public void saveAllAdditionalFields(List<PatentAdditionalField> additionalFields) {
        additionalFieldRepository.saveAll(additionalFields);
    }

    public Patent getPatentById(Long id) {
        return patentRepository.findById(id).orElse(null);
    }

    @Transactional
    public boolean isPatentExists(Patent patent) {
        if (patent == null) return false;

        String securityDocNumber = patent.getSecurityDocNumber();
        String registrationNumber = patent.getRegistrationNumber();
        String category = patent.getCategory();

        if (securityDocNumber == null && registrationNumber == null) return false;

        return (securityDocNumber != null) ?
                patentRepository.findBySecurityDocNumberAndCategory(securityDocNumber, category).isPresent() :
                patentRepository.findByRegistrationNumberAndCategory(registrationNumber, category).isPresent();
    }
}
