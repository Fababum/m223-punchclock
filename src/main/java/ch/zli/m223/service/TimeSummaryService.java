package ch.zli.m223.service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import jakarta.enterprise.context.ApplicationScoped;

import ch.zli.m223.dto.TimeSummaryDto;
import ch.zli.m223.model.Entry;

@ApplicationScoped
public class TimeSummaryService {

    public List<TimeSummaryDto> calculateSummaryPerDay(List<Entry> entries) {
        Map<LocalDate, Duration> perDay = new TreeMap<>();
        for (Entry entry : entries) {
            if (entry.getCheckIn() == null || entry.getCheckOut() == null) {
                continue;
            }
            Duration duration = Duration.between(entry.getCheckIn(), entry.getCheckOut());
            perDay.merge(entry.getCheckIn().toLocalDate(), duration, Duration::plus);
        }
        return perDay.entrySet().stream()
                .map(e -> new TimeSummaryDto(e.getKey(), e.getValue()))
                .toList();
    }
}
