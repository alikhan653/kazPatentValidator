package kz.it.patentparser.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "doc_numbers")
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DocNumber {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String category;
    private int documentNumber;

    public DocNumber(String category, int documentNumber) {
        this.category = category;
        this.documentNumber = documentNumber;
    }
}