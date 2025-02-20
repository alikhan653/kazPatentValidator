package kz.it.patentparser.model;

import jakarta.persistence.*;
import lombok.*;

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
    private String title;  // Название патента
    private String applicationNumber;  // Номер заявки
    private String filingDate;  // Дата подачи
    @Column(length = 500)
    private String authors;  // Авторы
    private String patentHolder;  // Патентообладатель
    private String securityDocNumber;  // Номер охранного документа
    private String status;  // Статус
    private String ipc;  // МПК
    private String bulletinNumber;  // Номер бюллетеня
    private String bulletinDate;  // Дата бюллетеня
    private String typeOfDocument;  // Тип документа

    public Patent(String title, String applicationNumber, String filingDate, String authors, String patentHolder, String securityDocNumber, String status, String ipc, String bulletinNumber, String bulletinDate, String typeOfDocument) {
        this.title = title;
        this.applicationNumber = applicationNumber;
        this.filingDate = filingDate;
        this.authors = authors;
        this.patentHolder = patentHolder;
        this.securityDocNumber = securityDocNumber;
        this.status = status;
        this.ipc = ipc;
        this.bulletinNumber = bulletinNumber;
        this.bulletinDate = bulletinDate;
        this.typeOfDocument = typeOfDocument;
    }
}
