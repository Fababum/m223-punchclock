package ch.zli.m223.controller;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;

import ch.zli.m223.dto.TagDto;
import ch.zli.m223.model.Tag;
import ch.zli.m223.service.TagService;

@Path("/tags")
@org.eclipse.microprofile.openapi.annotations.tags.Tag(name = "Tags", description = "Handling of tags")
public class TagController {

    @Inject
    TagService tagService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Index all tags.", description = "Returns a list of all tags.")
    public List<TagDto> index() {
        return tagService.findAll().stream().map(TagDto::fromEntity).toList();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Creates a new tag.", description = "Creates a new tag and returns the newly added tag.")
    public TagDto create(@Valid TagDto tagDto) {
        Tag tag = tagService.createTag(TagDto.toEntity(tagDto));
        return TagDto.fromEntity(tag);
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete a tag.", description = "Deletes an existing tag.")
    public Response delete(@PathParam("id") Long id) {
        if (!tagService.deleteTag(id)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.noContent().build();
    }

    @PUT
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Edit a tag.", description = "Edits an existing tag and returns the updated tag.")
    public Response put(@PathParam("id") Long id, @Valid TagDto tagDto) {
        Tag updated = tagService.updateTag(id, TagDto.toEntity(tagDto));
        if (updated == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(TagDto.fromEntity(updated)).build();
    }
}
