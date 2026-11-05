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
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import ch.zli.m223.dto.EntryDto;
import ch.zli.m223.model.Entry;
import ch.zli.m223.service.EntryService;

@Path("/entries")
@Tag(name = "Entries", description = "Handling of entries")
public class EntryController {

    @Inject
    EntryService entryService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Index all Entries.", description = "Returns a list of all entries.")
    public List<EntryDto> index() {
        return entryService.findAll().stream().map(EntryDto::fromEntity).toList();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Creates a new entry.", description = "Creates a new entry and returns the newly added entry.")
    public EntryDto create(@Valid EntryDto entryDto) {
        Entry entry = entryService.createEntry(EntryDto.toEntity(entryDto));
        return EntryDto.fromEntity(entry);
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete an entry.", description = "Delete a created entry.")
    public Response delete(@PathParam("id") Long id) {
        if (!entryService.deleteEntry(id)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.noContent().build();
    }

    @PUT
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Edit an entry.", description = "Edit a created entry and returns the updated entry.")
    public Response put(@PathParam("id") Long id, @Valid EntryDto entryDto) {
        Entry updated = entryService.updateEntry(id, EntryDto.toEntity(entryDto));
        if (updated == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(EntryDto.fromEntity(updated)).build();
    }
}
