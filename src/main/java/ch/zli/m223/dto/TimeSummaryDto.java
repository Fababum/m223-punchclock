package ch.zli.m223.dto;

import java.time.Duration;
import java.time.LocalDate;

public record TimeSummaryDto(LocalDate date, Duration duration) {
}
