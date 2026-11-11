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

import ch.zli.m223.dto.CategoryDto;
import ch.zli.m223.model.Category;
import ch.zli.m223.service.CategoryService;

@Path("/categories")
@Tag(name = "Categories", description = "Handling of categories")
public class CategoryController {

    @Inject
    CategoryService categoryService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Index all categories.", description = "Returns a list of all categories.")
    public List<CategoryDto> index() {
        return categoryService.findAll().stream().map(CategoryDto::fromEntity).toList();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Creates a new category.", description = "Creates a new category and returns the newly added category.")
    public CategoryDto create(@Valid CategoryDto categoryDto) {
        Category category = categoryService.createCategory(CategoryDto.toEntity(categoryDto));
        return CategoryDto.fromEntity(category);
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete a category.", description = "Deletes an existing category.")
    public Response delete(@PathParam("id") Long id) {
        if (!categoryService.deleteCategory(id)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.noContent().build();
    }

    @PUT
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Edit a category.", description = "Edits an existing category and returns the updated category.")
    public Response put(@PathParam("id") Long id, @Valid CategoryDto categoryDto) {
        Category updated = categoryService.updateCategory(id, CategoryDto.toEntity(categoryDto));
        if (updated == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(CategoryDto.fromEntity(updated)).build();
    }
}
