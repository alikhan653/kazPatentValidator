package kz.it.patentparser.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "patent_additional_fields")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PatentAdditionalField {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "patent_id", nullable = false)
    private Patent patent;
    
    private String label;
    private String value;

    public PatentAdditionalField(Patent patent, String label, String value) {
        this.patent = patent;
        this.label = label;
        this.value = value;
    }

    // Getters and setters
}