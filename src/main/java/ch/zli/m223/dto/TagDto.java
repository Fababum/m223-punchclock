package ch.zli.m223.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import ch.zli.m223.model.Tag;

public class TagDto {
    @Schema(readOnly = true)
    private Long id;

    @NotBlank(message = "Tag needs a title")
    private String title;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public static Tag toEntity(TagDto dto) {
        Tag tag = new Tag();
        tag.setTitle(dto.getTitle());
        return tag;
    }

    public static TagDto fromEntity(Tag tag) {
        TagDto dto = new TagDto();
        dto.setId(tag.getId());
        dto.setTitle(tag.getTitle());
        return dto;
    }
}
