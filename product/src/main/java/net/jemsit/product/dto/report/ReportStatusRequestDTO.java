package net.jemsit.product.dto.report;

import jakarta.validation.constraints.NotNull;
import net.jemsit.product.data.model.report.ReportStatus;

public record ReportStatusRequestDTO(@NotNull ReportStatus status) {
}
