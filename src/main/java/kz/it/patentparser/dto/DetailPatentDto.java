package kz.it.patentparser.dto;

import jakarta.persistence.Column;
import kz.it.patentparser.model.Patent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DetailPatentDto {
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
    private String patentSite;
    private String imageUrl;
    private List<AdditionalFieldDto> additionalFields;

    public static DetailPatentDto fromEntity(Patent patent) {
        return new DetailPatentDto(
                patent.getId(),
                patent.getSecurityDocNumber(),
                patent.getRegistrationNumber(),
                patent.getStatus(),
                patent.getApplicationNumber(),
                patent.getFilingDate(),
                patent.getRegistrationDate(),
                patent.getExpirationDate(),
                patent.getBulletinDate(),
                patent.getTitle(),
                patent.getName(),
                patent.getBulletinNumber(),
                patent.getIpc(),
                patent.getMkpo(),
                patent.getSortName(),
                patent.getPatentHolder(),
                patent.getAuthors(),
                patent.getOwner(),
                patent.getCategory(),
                patent.getPatentSite(),
                patent.getImageUrl(),
                patent.getAdditionalFields().stream()
                        .map(AdditionalFieldDto::fromEntity)
                        .collect(Collectors.toList())
        );
    }
}
