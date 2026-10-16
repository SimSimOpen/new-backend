package net.jemsit.common.dto.response.dashboard;

import java.util.List;

/** Account numbers for the admin dashboard (admins and managers only). */
public record UserDashboardDTO(
        long clients,
        long agents,
        long newClientsThisMonth,
        List<MonthlyCountDTO> signupsPerMonth,
        List<ActivityItemDTO> activity
) {
}
