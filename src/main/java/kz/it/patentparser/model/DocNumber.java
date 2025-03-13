package kz.it.patentparser.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "doc_numbers", indexes = {@Index(name = "idx_doc_number", columnList = "documentNumber")})
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DocNumber {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String category;
    private String documentNumber;
    private boolean isParsed;
    public DocNumber(String category, String documentNumber, boolean isParsed) {
        this.category = category;
        this.documentNumber = documentNumber;
        this.isParsed = isParsed;
    }
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public boolean isParsed() {
        return isParsed;
    }

    public void setParsed(boolean parsed) {
        isParsed = parsed;
    }
}