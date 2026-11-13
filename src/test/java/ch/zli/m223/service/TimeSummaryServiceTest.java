package ch.zli.m223.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.zli.m223.dto.TimeSummaryDto;
import ch.zli.m223.model.Entry;

public class TimeSummaryServiceTest {

    private TimeSummaryService timeSummaryService;

    @BeforeEach
    void setUp() {
        timeSummaryService = new TimeSummaryService();
    }

    @Test
    void calculateSummaryPerDay_emptyList_returnsEmptyList() {
        List<TimeSummaryDto> result = timeSummaryService.calculateSummaryPerDay(List.of());

        assertTrue(result.isEmpty());
    }

    @Test
    void calculateSummaryPerDay_nullList_returnsEmptyList() {
        List<TimeSummaryDto> result = timeSummaryService.calculateSummaryPerDay(null);

        assertTrue(result.isEmpty());
    }

    @Test
    void calculateSummaryPerDay_singleEntry_returnsCorrectDurationForDay() {
        Entry entry = createEntry(
                LocalDateTime.of(2026, 9, 29, 8, 0),
                LocalDateTime.of(2026, 9, 29, 17, 0));

        List<TimeSummaryDto> result = timeSummaryService.calculateSummaryPerDay(List.of(entry));

        assertEquals(
                List.of(new TimeSummaryDto(LocalDate.of(2026, 9, 29), Duration.ofHours(9))),
                result);
    }

    @Test
    void calculateSummaryPerDay_multipleEntriesSameDay_sumsDurations() {
        Entry morning = createEntry(
                LocalDateTime.of(2026, 9, 29, 8, 0),
                LocalDateTime.of(2026, 9, 29, 12, 0));
        Entry afternoon = createEntry(
                LocalDateTime.of(2026, 9, 29, 13, 0),
                LocalDateTime.of(2026, 9, 29, 17, 30));

        List<TimeSummaryDto> result = timeSummaryService.calculateSummaryPerDay(List.of(morning, afternoon));

        assertEquals(
                List.of(new TimeSummaryDto(LocalDate.of(2026, 9, 29), Duration.ofHours(8).plusMinutes(30))),
                result);
    }

    @Test
    void calculateSummaryPerDay_multipleDaysUnsorted_returnsSortedSummariesPerDay() {
        Entry secondDay = createEntry(
                LocalDateTime.of(2026, 9, 30, 9, 0),
                LocalDateTime.of(2026, 9, 30, 12, 0));
        Entry firstDay = createEntry(
                LocalDateTime.of(2026, 9, 28, 8, 0),
                LocalDateTime.of(2026, 9, 28, 13, 0));

        List<TimeSummaryDto> result = timeSummaryService.calculateSummaryPerDay(List.of(secondDay, firstDay));

        assertEquals(
                List.of(
                        new TimeSummaryDto(LocalDate.of(2026, 9, 28), Duration.ofHours(5)),
                        new TimeSummaryDto(LocalDate.of(2026, 9, 30), Duration.ofHours(3))),
                result);
    }

    @Test
    void calculateSummaryPerDay_incompleteOrNullEntries_ignoresInvalidAndCalculatesValid() {
        Entry missingCheckIn = createEntry(null, LocalDateTime.of(2026, 9, 29, 12, 0));
        Entry missingCheckOut = createEntry(LocalDateTime.of(2026, 9, 29, 8, 0), null);
        Entry validEntry = createEntry(
                LocalDateTime.of(2026, 9, 29, 13, 0),
                LocalDateTime.of(2026, 9, 29, 15, 0));

        List<TimeSummaryDto> result = timeSummaryService.calculateSummaryPerDay(
                Arrays.asList(null, missingCheckIn, missingCheckOut, validEntry));

        assertEquals(
                List.of(new TimeSummaryDto(LocalDate.of(2026, 9, 29), Duration.ofHours(2))),
                result);
    }

    @Test
    void calculateSummaryPerDay_checkOutBeforeCheckIn_throwsIllegalArgumentException() {
        Entry invalidEntry = createEntry(
                LocalDateTime.of(2026, 9, 29, 17, 0),
                LocalDateTime.of(2026, 9, 29, 8, 0));

        assertThrows(
                IllegalArgumentException.class,
                () -> timeSummaryService.calculateSummaryPerDay(List.of(invalidEntry)));
    }

    @Test
    void calculateSummaryPerDay_overnightEntry_splitsDurationAcrossDays() {
        Entry overnightEntry = createEntry(
                LocalDateTime.of(2026, 9, 29, 22, 0),
                LocalDateTime.of(2026, 9, 30, 2, 0));

        List<TimeSummaryDto> result = timeSummaryService.calculateSummaryPerDay(List.of(overnightEntry));

        assertEquals(
                List.of(
                        new TimeSummaryDto(LocalDate.of(2026, 9, 29), Duration.ofHours(2)),
                        new TimeSummaryDto(LocalDate.of(2026, 9, 30), Duration.ofHours(2))),
                result);
    }

    private static Entry createEntry(LocalDateTime checkIn, LocalDateTime checkOut) {
        Entry entry = new Entry();
        entry.setCheckIn(checkIn);
        entry.setCheckOut(checkOut);
        return entry;
    }
}
