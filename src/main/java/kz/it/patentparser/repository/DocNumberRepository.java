package kz.it.patentparser.repository;

import kz.it.patentparser.model.DocNumber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DocNumberRepository extends JpaRepository<DocNumber, Long> {
    boolean existsByCategoryAndDocumentNumber(String category, String documentNumber);
    List<DocNumber> findByIsParsedFalse();

    @Query("SELECT d FROM DocNumber d WHERE NOT EXISTS (SELECT 1 FROM PatentAdditionalField p WHERE p.patent.docNumber = d.documentNumber AND p.patent.category = 'Товарные знаки' AND p.label = 'imageBase64')")
    List<DocNumber> findPatentsWithoutImages();
}
