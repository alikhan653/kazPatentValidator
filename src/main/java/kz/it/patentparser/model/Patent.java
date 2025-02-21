package kz.it.patentparser.model;

import jakarta.persistence.*;
import lombok.*;
import org.checkerframework.checker.units.qual.Length;

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
    @Column(length = 1000)
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
    @Column(length = 1000)
    private String patentHolder;
    @Column(length = 1000)
    private String authors;
    @Column(length = 1000)
    private String owner;
    @Column(length = 50)
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

