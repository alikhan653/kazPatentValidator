package kz.it.patentparser.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

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

    @JsonProperty("field_31")
    private String field31;

    @JsonProperty("field_32")
    private String field32;

    @JsonProperty("field_33")
    private String field33;

    @JsonProperty("date_85")
    private String date85;

    @JsonProperty("field_86")
    private String field86;

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
}