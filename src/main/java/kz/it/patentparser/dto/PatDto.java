package kz.it.patentparser.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
public class PatDto {
    private Long id;
    private String title;
    private String securityDocNumber;
    private String status;
    private String owner;
    private LocalDate registrationDate;
    private String registrationNumber;
    private String image;
}
