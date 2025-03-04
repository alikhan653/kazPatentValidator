package kz.it.patentparser.repository;

import kz.it.patentparser.model.Patent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PatentRepository extends JpaRepository<Patent, Long>, JpaSpecificationExecutor<Patent> {
    Optional<Patent> findByApplicationNumber(String applicationNumber);
    Optional<Patent> findBySecurityDocNumberAndCategory(String securityDocNumber, String category);
    @Query("SELECT p FROM Patent p WHERE " +
            "(LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(p.title) LIKE LOWER(CONCAT('%', :transliteratedQuery1, '%')) " +
            "OR LOWER(p.title) LIKE LOWER(CONCAT('%', :transliteratedQuery2, '%'))) " +
            "AND (p.expirationDate >= :startDate " +
            "AND p.expirationDate <= :endDate) " +
            "AND ((COALESCE(:siteType, '') = '') OR p.patentSite = :siteType) " +
            "AND (:expired IS NULL OR (:expired = TRUE AND (p.registrationDate <= :todayMinus10Years)) " +
            "OR  (:expired = FALSE AND (p.registrationDate > :todayMinus10Years))) " +
            "AND ((COALESCE(:category, '') = '') OR p.category = :category)" +
            "ORDER BY " +
            "CASE WHEN p.expirationDate IS NULL THEN 1 ELSE 0 END, " + // Push NULLs to the end
            "p.expirationDate DESC, " +
            "p.id DESC" // Keep original sorting as secondary criteria
    )
    Page<Patent> searchPatents(
            @Param("query") String query,
            @Param("transliteratedQuery1") String transliteratedQuery1,
            @Param("transliteratedQuery2") String transliteratedQuery2,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("siteType") String siteType,
            @Param("expired") Boolean expired,
            @Param("todayMinus10Years") LocalDate todayMinus10Years,
            @Param("category") String category,
            Pageable pageable
    );
    Optional<Patent> findByRegistrationNumberAndCategory(String registrationNumber, String category);
    List<Patent> findByTitleContainingAndRegistrationDateBetween(String title, LocalDate startDate, LocalDate endDate);
    //expired value should check date from registrationDate, if it's more than 10 years, then it's expired

    @Query("SELECT p FROM Patent p WHERE " +
            "(" +
            // Если нет поискового запроса, не фильтруем
            "(COALESCE(:query, '') = '' AND COALESCE(:transliteratedQuery1, '') = '' AND COALESCE(:transliteratedQuery2, '') = '') " +
            "OR LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(p.title) LIKE LOWER(CONCAT('%', :transliteratedQuery1, '%')) " +
            "OR LOWER(p.title) LIKE LOWER(CONCAT('%', :transliteratedQuery2, '%')) " +
            ") " +

            "AND (" +
            // Если нет дат, не фильтруем
            "(COALESCE(:startDate, '1800-01-01') = '1800-01-01' AND COALESCE(:endDate, '2100-01-01') = '2100-01-01')" +
            "OR (p.expirationDate IS NULL OR (p.expirationDate >= :startDate AND p.expirationDate <= :endDate))" +
            ") " +

            "AND (" +
            // Если нет типа сайта, не фильтруем
            "COALESCE(:siteType, '') = '' OR p.patentSite IS NULL OR p.patentSite = :siteType" +
            ") " +

            "AND (" +
            // Если нет фильтра expired, не фильтруем
            ":expired IS NULL OR " +
            "(:expired = TRUE AND (p.registrationDate IS NULL OR p.registrationDate <= :todayMinus10Years)) OR " +
            "(:expired = FALSE AND (p.registrationDate IS NULL OR p.registrationDate > :todayMinus10Years))" +
            ") " +

            "AND (" +
            // Если нет категории, не фильтруем
            "COALESCE(:category, '') = '' OR p.category IS NULL OR p.category = :category" +
            ")"
    )
    Page<Patent> searchPatents1(@Param("query") String query,
                               @Param("transliteratedQuery1") String transliteratedQuery1,
                               @Param("transliteratedQuery2") String transliteratedQuery2,
                               @Param("startDate") LocalDate startDate,
                               @Param("endDate") LocalDate endDate,
                               @Param("siteType") String siteType,
                               @Param("expired") Boolean expired,
                               @Param("todayMinus10Years") LocalDate todayMinus10Years,
                               @Param("category") String category,
                               Pageable pageable);
    @Query("SELECT p FROM Patent p WHERE " +
            "(LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(p.title) LIKE LOWER(CONCAT('%', :transliteratedQuery1, '%')) " +
            "OR LOWER(p.title) LIKE LOWER(CONCAT('%', :transliteratedQuery2, '%'))) " +
            "AND (COALESCE(:startDate, '1900-01-01') = '1900-01-01' OR p.expirationDate >= :startDate) " +
            "AND (COALESCE(:endDate, '2100-01-01') = '2100-01-01' OR p.expirationDate <= :endDate) " +
            "AND (COALESCE(:siteType, '') = '' OR p.patentSite = :siteType) " +
            "AND (:expired IS NULL OR (:expired = TRUE AND p.registrationDate <= :todayMinus10Years) " +
            "OR  (:expired = FALSE AND p.registrationDate > :todayMinus10Years)) " +
            "AND (COALESCE(:category, '') = '' OR p.category = :category)"
    )
    List<Patent> findAllMatchingPatents(
            @Param("query") String query,
            @Param("transliteratedQuery1") String transliteratedQuery1,
            @Param("transliteratedQuery2") String transliteratedQuery2,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("siteType") String siteType,
            @Param("expired") Boolean expired,
            @Param("todayMinus10Years") LocalDate todayMinus10Years,
            @Param("category") String category
    );


}
