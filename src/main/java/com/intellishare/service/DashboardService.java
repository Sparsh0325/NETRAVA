package com.intellishare.service;
import com.intellishare.dto.dashboard.DashboardStatsResponse;
import com.intellishare.model.User;
import com.intellishare.repository.InvestigationCaseRepository;
import org.springframework.stereotype.Service;
@Service
public class DashboardService {
    private final InvestigationCaseRepository cases;
    public DashboardService(InvestigationCaseRepository cases){this.cases=cases;}
    public DashboardStatsResponse stats(User actor){
        boolean admin="Administrator".equals(actor.getRole());
        return new DashboardStatsResponse(
            admin?cases.count():cases.countByAgency(actor.getAgency()),
            admin?cases.countByStatus("Under Investigation"):cases.countByAgencyAndStatus(actor.getAgency(),"Under Investigation"),
            admin?cases.countByPriority("High"):cases.countByAgencyAndPriority(actor.getAgency(),"High"),
            admin?cases.countByStatus("Closed"):cases.countByAgencyAndStatus(actor.getAgency(),"Closed"));
    }
}
