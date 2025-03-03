package kz.it.patentparser.dto;

import kz.it.patentparser.model.PatentAdditionalField;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AdditionalFieldDto {
    private String label;
    private String value;

    public static AdditionalFieldDto fromEntity(PatentAdditionalField field) {
        return new AdditionalFieldDto(field.getLabel(), field.getValue());
    }
}
