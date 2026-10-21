package com.intellishare.repository;
import com.intellishare.casefile.InvestigationCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;
public interface InvestigationCaseRepository extends JpaRepository<InvestigationCase,Long>, JpaSpecificationExecutor<InvestigationCase> {
    Optional<InvestigationCase> findByCaseNumberIgnoreCase(String caseNumber);
    boolean existsByCaseNumberIgnoreCase(String caseNumber);
    long countByAgency(String agency);
    long countByAgencyAndStatus(String agency,String status);
    long countByAgencyAndPriority(String agency,String priority);
    long countByStatus(String status);
    long countByPriority(String priority);
}
