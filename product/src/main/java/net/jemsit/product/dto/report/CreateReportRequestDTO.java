package net.jemsit.product.dto.report;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.jemsit.product.data.model.engagement.ListingKind;
import net.jemsit.product.data.model.report.ListingReport;
import net.jemsit.product.data.model.report.ReportReason;

public record CreateReportRequestDTO(@NotNull ListingKind kind,
                                     @NotNull Long listingId,
                                     @NotNull ReportReason reason,
                                     @Size(max = ListingReport.MAX_DETAILS) String details) {
}
