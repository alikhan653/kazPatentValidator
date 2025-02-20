package kz.it.patentparser.repository;

import kz.it.patentparser.model.PatentAdditionalField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PatentAdditionalFieldRepository extends JpaRepository<PatentAdditionalField, Long> {}