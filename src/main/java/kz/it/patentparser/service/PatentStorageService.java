package kz.it.patentparser.service;

import kz.it.patentparser.model.DocNumber;
import kz.it.patentparser.repository.DocNumberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;

@Service
public class PatentStorageService {
    private static final Logger logger = LoggerFactory.getLogger(PatentStorageService.class);

    private final DocNumberRepository docNumberRepository;

    public PatentStorageService(DocNumberRepository docNumberRepository) {
        this.docNumberRepository= docNumberRepository;
    }


    public synchronized void saveDocNumber(String category, String docNumber, boolean isParsed) {
        try {
            DocNumber docNumberObject = new DocNumber(category, docNumber, isParsed);
            //check if patent already exists
            if (docNumberRepository.existsByCategoryAndDocumentNumber(category, docNumber)) {
                logger.info("Patent {} - {} already exists in database", category, docNumber);
                return;
            }
            docNumberRepository.save(docNumberObject);
            logger.info("Saved patent {} - {} to database", category, docNumber);
        } catch (Exception e) {
            logger.error("Error saving to database: {}", e.getMessage());
        }
    }
}