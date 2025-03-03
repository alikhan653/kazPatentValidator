package kz.it.patentparser.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.stream.Collectors;

@Data
public class PatentDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("pat_dby")
    private String publicationDate;

    @JsonProperty("pat_nby")
    private String bulletinNumber;

    @JsonProperty("pat_uid")
    private Long uniqueId;

    @JsonProperty("pat_type_id")
    private String patentTypeId;

    @JsonProperty("code_13")
    private String code13;

    @JsonProperty("gos_number_11")
    private String securityDocNumber;
    @JsonProperty("gos_date_11")
    private String securityDocDate;
    @JsonProperty("icp_51")
    private String ipcCodes;

    @JsonProperty("req_number_21")
    private String applicationNumber;

    @JsonProperty("req_date_22")
    private String applicationDate;

    @JsonProperty("pat_paro")
    private String priorityNumber;

    @JsonProperty("dvidod")
    private String additionalInfo1;
    @JsonProperty("date_85")
    private String date85;
    @JsonProperty("author_72_kz")
    private String authorsKz;

    @JsonProperty("author_72_ru")
    private String authorsRu;

    @JsonProperty("owner_73_kz")
    private String ownerKz;

    @JsonProperty("owner_73_ru")
    private String ownerRu;

    @JsonProperty("attorney_74_kz")
    private String attorneyKz;

    @JsonProperty("attorney_74_ru")
    private String attorneyRu;

    @JsonProperty("name_540_ru")
    private String titleRu;

    @JsonProperty("name_540_kz")
    private String titleKz;

    @JsonProperty("ref_57")
    private String description;
    @JsonProperty("field_31")
    private String field31;
    @JsonProperty("field_32")
    private String field32;
    @JsonProperty("field_33")
    private String field33;
    @JsonProperty("field_86")
    private String field86;
    @JsonProperty("field_181")
    private String field181;
    @JsonProperty("field_510_511_short")
    private String field510511Short;
    @JsonProperty("field_510_511")
    private String field510511;
    @JsonProperty("field_526_ru")
    private String field526Ru;
    @JsonProperty("field_526_kz")
    private String field526Kz;
    @JsonProperty("field_591")
    private String field591;
    @JsonProperty("field_730_ru")
    private String field730Ru;
    @JsonProperty("field_730_kz")
    private String field730Kz;

    private String imageBase64;
    private String category;
}