package ch.zli.m223.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import ch.zli.m223.model.Entry;
import ch.zli.m223.model.Tag;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public class EntryDto {

    @Schema(readOnly = true)
    private Long id;

    @Schema(examples = {"2022-03-10T08:00:00"})
    @NotNull(message = "checkIn is required")
    private LocalDateTime checkIn;

    @Schema(examples = {"2022-03-10T17:00:00"})
    @NotNull(message = "checkOut is required")
    private LocalDateTime checkOut;

    private Long categoryId;

    private List<Long> tagIds;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(LocalDateTime checkIn) {
        this.checkIn = checkIn;
    }

    public LocalDateTime getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(LocalDateTime checkOut) {
        this.checkOut = checkOut;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public List<Long> getTagIds() {
        return tagIds;
    }

    public void setTagIds(List<Long> tagIds) {
        this.tagIds = tagIds;
    }

    @Schema(hidden = true)
    @AssertTrue(message = "checkIn must be before checkOut")
    public boolean isCheckInBeforeCheckOut() {
        if (checkIn == null || checkOut == null) {
            return true;
        }
        return checkIn.isBefore(checkOut);
    }

    public static Entry toEntity(EntryDto dto) {
        Entry entry = new Entry();
        entry.setCheckIn(dto.getCheckIn());
        entry.setCheckOut(dto.getCheckOut());
        return entry;
    }

    public static EntryDto fromEntity(Entry entry) {
        EntryDto dto = new EntryDto();
        dto.setId(entry.getId());
        dto.setCheckIn(entry.getCheckIn());
        dto.setCheckOut(entry.getCheckOut());
        if (entry.getCategory() != null) {
            dto.setCategoryId(entry.getCategory().getId());
        }
        if (entry.getTags() != null) {
            dto.setTagIds(entry.getTags().stream().map(Tag::getId).sorted().toList());
        } else {
            dto.setTagIds(List.of());
        }
        return dto;
    }
}
