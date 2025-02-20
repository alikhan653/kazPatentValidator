package kz.it.patentparser.service;

import jakarta.transaction.Transactional;
import kz.it.patentparser.model.Patent;
import kz.it.patentparser.model.PatentAdditionalField;
import kz.it.patentparser.repository.PatentAdditionalFieldRepository;
import kz.it.patentparser.repository.PatentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PatentService {
    private final PatentRepository patentRepository;
    private final PatentAdditionalFieldRepository additionalFieldRepository;

    @Autowired
    public PatentService(PatentRepository patentRepository, PatentAdditionalFieldRepository additionalFieldRepository) {
        this.patentRepository = patentRepository;
        this.additionalFieldRepository = additionalFieldRepository;
    }

    @Transactional
    public Patent savePatent(Patent patent) {
        return patentRepository.save(patent);
    }

    @Transactional
    public void savePatents(List<Patent> patents) {
        patentRepository.saveAll(patents);
    }

    @Transactional
    public void saveAdditionalFields(PatentAdditionalField additionalField) {
        additionalFieldRepository.save(additionalField);
    }

    @Transactional
    public boolean isPatentExists(Patent patent) {
        return patentRepository.findBySecurityDocNumberOrRegistrationNumber(patent.getSecurityDocNumber(), patent.getRegistrationNumber()).isPresent();
    }
}