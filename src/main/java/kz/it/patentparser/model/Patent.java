package kz.it.patentparser.model;

import jakarta.persistence.*;
import lombok.*;
import org.checkerframework.checker.units.qual.Length;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "patents", indexes = {@Index(name = "idx_registration_number", columnList = "registrationNumber"), @Index(name = "idx_security_doc_number", columnList = "securityDocNumber"), @Index(name = "idx_category", columnList = "category")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Patent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String securityDocNumber;
    private String registrationNumber;
    private String status;
    private String applicationNumber;
    private LocalDate filingDate;
    private LocalDate registrationDate;
    private LocalDate expirationDate;
    private LocalDate bulletinDate;
    @Column(columnDefinition = "TEXT")
    private String title;
    @Column(length = 1000)
    private String name;
    private String bulletinNumber;
    @Column(length = 1000)
    private String ipc;
    @Column(length = 1000)
    private String mkpo;
    @Column(length = 500)
    private String sortName;
    @Column(columnDefinition = "TEXT")
    private String patentHolder;
    @Column(columnDefinition = "TEXT")
    private String authors;
    @Column(columnDefinition = "TEXT")
    private String owner;
    @Column(length = 50)
    private String category;
    private String patentSite;
    private String imageUrl;
    private String docNumber;

    @OneToMany(mappedBy = "patent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PatentAdditionalField> additionalFields;

    // Getters and setters


    public Patent(String status, String bulletinNumber, LocalDate bulletinDate, String category) {
        this.status = status;
        this.bulletinNumber = bulletinNumber;
        this.bulletinDate = bulletinDate;
        this.category = category;
    }

    @Override
    public String toString() {
        return "Patent{id=" + id +
                ", title='" + title + '\'' +
                ", category='" + category + '\'' +
                ", filingDate=" + filingDate +
                ", securityDocNumber='" + securityDocNumber + '\'' +
                ", additionalFields=" + (additionalFields != null ? additionalFields.size() : "null") +
                '}';
    }
}

