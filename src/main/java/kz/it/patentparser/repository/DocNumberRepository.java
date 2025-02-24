package kz.it.patentparser.repository;

import kz.it.patentparser.model.DocNumber;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocNumberRepository extends JpaRepository<DocNumber, Long> {
    boolean existsByCategoryAndDocumentNumber(String category, int documentNumber);
}
