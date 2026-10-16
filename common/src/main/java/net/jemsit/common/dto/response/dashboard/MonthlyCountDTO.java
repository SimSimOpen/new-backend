package net.jemsit.common.dto.response.dashboard;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** How many things happened in one calendar month; {@code month} is "yyyy-MM". */
public record MonthlyCountDTO(String month, long count) {

    /**
     * The last {@code months} months up to and including the current one, oldest first, with months
     * missing from {@code counts} (keyed "yyyy-MM") reported as zero so a chart has no gaps.
     */
    public static List<MonthlyCountDTO> lastMonths(int months, Map<String, Long> counts) {
        YearMonth current = YearMonth.now();
        List<MonthlyCountDTO> result = new ArrayList<>(months);
        for (int i = months - 1; i >= 0; i--) {
            String key = current.minusMonths(i).toString();
            result.add(new MonthlyCountDTO(key, counts.getOrDefault(key, 0L)));
        }
        return result;
    }
}
