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
import kz.it.patentparser.util.PatentSpecifications;
import kz.it.patentparser.util.TransliterationUtil;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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
    private final PatentApiClient patentApiClient;


    @Autowired
    public PatentService(PatentRepository patentRepository, PatentAdditionalFieldRepository additionalFieldRepository, PatentApiClient patentApiClient) {
        this.patentRepository = patentRepository;
        this.additionalFieldRepository = additionalFieldRepository;
        this.patentApiClient = patentApiClient;
    }

    @Cacheable(value = "patents", key = "#page + '-' + #size")
    public Page<Patent> getPatents(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        return patentRepository.findAll(pageable);
    }

    public Mono<String> getImageBase64(String patentId, String endpoint) {
        return patentApiClient.fetchImageBase64(patentId, endpoint);
    }

    public Optional<Patent> findById(Long id) {
        return patentRepository.findById(id);
    }


    @Cacheable(value = "patentsCache", key = "{#query, #startDate, #endDate, #siteType, #expired, #category, #mktu, #securityDocNumber, #pageable}")
    public Page<Patent> searchPatents(String query, LocalDate startDate, LocalDate endDate, String siteType, Boolean expired, String category, String mktu, String securityDocNumber, Pageable pageable) {
        List<Specification<Patent>> specs = new ArrayList<>();
        logger.info("Searching patents with params: query={}, startDate={}, endDate={}, siteType={}, expired={}, category={}, mktu={}, securityDocNumber={}",
                query, startDate, endDate, siteType, expired, category, mktu, securityDocNumber);
        if (query != null && !query.isEmpty()) specs.add(PatentSpecifications.hasTitle(query));
        if (startDate != null || endDate != null) specs.add(PatentSpecifications.hasExpirationDateBetween(startDate, endDate));
        if (siteType != null && !siteType.isEmpty()) specs.add(PatentSpecifications.hasSiteType(siteType));
        if (category != null && !category.equals("0")) specs.add(PatentSpecifications.hasCategory(category));
        if (mktu != null && !mktu.isEmpty()) specs.add(PatentSpecifications.hasMktu(mktu));
        if (securityDocNumber != null && !securityDocNumber.isEmpty()) specs.add(PatentSpecifications.hasSecurityDocNumber(securityDocNumber));
        if (expired != null) specs.add(PatentSpecifications.isExpired(expired, LocalDate.now().minusYears(10)));

        Specification<Patent> finalSpec = specs.stream().reduce(Specification::and).orElse(null);
        Specification<Patent> sortedSpec = finalSpec != null ? finalSpec.and(PatentSpecifications.customSorting())
                : PatentSpecifications.customSorting();

        logger.info("Final spec: {}", finalSpec);
        logger.info("Pageable: {}", pageable);
        return patentRepository.findAll(sortedSpec, pageable);
    }


    @CacheEvict(value = "patentsCache", allEntries = true)
    public void clearPatentCache() {
        logger.info("Clearing patents cache...");
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

    public Patent getPatentByDocNumber(String docNumber) {
        return patentRepository.findByDocNumber(docNumber).orElse(null);
    }

    public List<Patent> findAllBySecurityDocNumberIn(List<String> securityDocNumbers) {
        return patentRepository.findAllBySecurityDocNumberIn(securityDocNumbers);
    }

    @Transactional
    public boolean isPatentExists(Patent patent) {
        if (patent == null) return false;

        String securityDocNumber = patent.getSecurityDocNumber();
        String registrationNumber = patent.getRegistrationNumber();
        String category = patent.getCategory();
        String siteType = patent.getPatentSite();

        if (securityDocNumber == null && registrationNumber == null) return false;

        return (securityDocNumber != null) ?
                patentRepository.findBySecurityDocNumberAndCategoryAndPatentSite(securityDocNumber, category, siteType).isPresent() :
                patentRepository.findByRegistrationNumberAndCategoryAndPatentSite(registrationNumber, category, siteType).isPresent();
    }
}
