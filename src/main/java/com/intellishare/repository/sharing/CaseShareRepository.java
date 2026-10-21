package com.intellishare.repository.sharing;

import com.intellishare.sharing.CaseShare;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CaseShareRepository extends JpaRepository<CaseShare,Long> {
    boolean existsByCaseRecordIdAndRecipientAgencyAndStatus(Long caseId,String agency,String status);
    boolean existsByCaseRecordIdAndRecipientAgencyAndPermissionAndStatus(Long caseId,String agency,String permission,String status);
    Optional<CaseShare> findByCaseRecordIdAndRecipientAgency(Long caseId,String agency);
    List<CaseShare> findTop500ByOwnerAgencyOrRecipientAgencyOrderByCreatedAtDesc(String ownerAgency,String recipientAgency);
    List<CaseShare> findTop500ByOrderByCreatedAtDesc();
}
