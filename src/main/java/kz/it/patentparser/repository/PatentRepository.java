package kz.it.patentparser.repository;

import kz.it.patentparser.model.Patent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatentRepository extends JpaRepository<Patent, Long> {
    Optional<Patent> findByApplicationNumber(String applicationNumber);
    Optional<Patent> findBySecurityDocNumber(String securityDocNumber);
    Optional<Patent> findByRegistrationNumber(String registrationNumber);

    Optional<Patent> findBySecurityDocNumberOrRegistrationNumber(String securityDocNumber, String registrationNumber);
}
