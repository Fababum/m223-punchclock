package ch.zli.m223.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import jakarta.enterprise.context.ApplicationScoped;

import ch.zli.m223.dto.TimeSummaryDto;
import ch.zli.m223.model.Entry;

@ApplicationScoped
public class TimeSummaryService {

    public List<TimeSummaryDto> calculateSummaryPerDay(List<Entry> entries) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }

        Map<LocalDate, Duration> perDay = new TreeMap<>();
        for (Entry entry : entries) {
            if (entry == null || entry.getCheckIn() == null || entry.getCheckOut() == null) {
                continue;
            }
            if (entry.getCheckOut().isBefore(entry.getCheckIn())) {
                throw new IllegalArgumentException("checkOut must not be before checkIn");
            }

            LocalDateTime current = entry.getCheckIn();
            LocalDateTime end = entry.getCheckOut();
            while (current.toLocalDate().isBefore(end.toLocalDate())) {
                LocalDateTime startOfNextDay = current.toLocalDate().plusDays(1).atStartOfDay();
                perDay.merge(current.toLocalDate(), Duration.between(current, startOfNextDay), Duration::plus);
                current = startOfNextDay;
            }
            if (!current.isEqual(end) || entry.getCheckIn().isEqual(end)) {
                perDay.merge(current.toLocalDate(), Duration.between(current, end), Duration::plus);
            }
        }
        return perDay.entrySet().stream()
                .map(e -> new TimeSummaryDto(e.getKey(), e.getValue()))
                .toList();
    }
}
