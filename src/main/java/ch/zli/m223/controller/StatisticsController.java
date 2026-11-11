package ch.zli.m223.controller;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import ch.zli.m223.dto.TimeSummaryDto;
import ch.zli.m223.service.EntryService;

@Path("/statistics")
@Tag(name = "Statistics", description = "Statistics about entries")
public class StatisticsController {

    @Inject
    EntryService entryService;

    @GET
    @Path("/time-summaries")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Time summaries per day.", description = "Returns the total recorded time per day (HH:mm:ss).")
    public Map<String, String> timeSummaries() {
        Map<String, String> result = new LinkedHashMap<>();
        for (TimeSummaryDto summary : entryService.getTimeSummaries()) {
            result.put(summary.date().toString(), format(summary.duration()));
        }
        return result;
    }

    private static String format(Duration d) {
        long seconds = d.getSeconds();
        return String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }
}
