package kz.it.patentparser.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "patents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
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
    private String title;
    private String name;
    private String bulletinNumber;
    private String ipc;
    private String mkpo;
    private String sortName;
    private String patentHolder;
    private String authors;
    private String owner;
    private String category;

    @OneToMany(mappedBy = "patent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PatentAdditionalField> additionalFields;

    // Getters and setters


    public Patent(String status, String bulletinNumber, LocalDate bulletinDate, String category) {
        this.status = status;
        this.bulletinNumber = bulletinNumber;
        this.bulletinDate = bulletinDate;
        this.category = category;
    }
}

