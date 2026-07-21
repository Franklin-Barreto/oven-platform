package br.com.f2e.ovenplatform.catalog.infrastructure.web;

import static br.com.f2e.ovenplatform.shared.infrastructure.web.ApiHeaders.API_VERSION_VALUE;

import br.com.f2e.ovenplatform.catalog.application.CategoryService;
import br.com.f2e.ovenplatform.identity.application.api.security.CurrentTenantId;
import br.com.f2e.ovenplatform.shared.infrastructure.web.ResourceUriBuilder;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/categories")
public class CategoryController {

  private final CategoryService categoryService;

  public CategoryController(CategoryService categoryService) {
    this.categoryService = categoryService;
  }

  @PreAuthorize("hasAuthority('CATALOG_MANAGE')")
  @PostMapping(version = API_VERSION_VALUE)
  public ResponseEntity<CategoryResponse> create(
      @CurrentTenantId UUID tenantId, @Valid @RequestBody CreateCategoryRequest categoryRequest) {
    var category = CategoryResponse.from(categoryService.save(tenantId, categoryRequest.name()));
    var uri = ResourceUriBuilder.buildLocation(category.id());
    return ResponseEntity.created(uri).body(category);
  }

  @PreAuthorize("hasAuthority('CATALOG_READ')")
  @GetMapping(version = API_VERSION_VALUE)
  public ResponseEntity<List<CategoryResponse>> list(@CurrentTenantId UUID tenantId) {
    var categories =
        categoryService.listCategories(tenantId).stream().map(CategoryResponse::from).toList();

    return ResponseEntity.ok(categories);
  }

  @PreAuthorize("hasAuthority('CATALOG_MANAGE')")
  @PatchMapping(version = API_VERSION_VALUE, path = "/{id}")
  public ResponseEntity<CategoryResponse> update(
      @CurrentTenantId UUID tenantId,
      @PathVariable UUID id,
      @Valid @RequestBody UpdateCategoryRequest categoryRequest) {
    var category =
        categoryService.update(tenantId, id, categoryRequest.name(), categoryRequest.active());

    return ResponseEntity.ok(CategoryResponse.from(category));
  }

  @PreAuthorize("hasAuthority('CATALOG_MANAGE')")
  @DeleteMapping(version = API_VERSION_VALUE, path = "/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@CurrentTenantId UUID tenantId, @PathVariable UUID id) {
    categoryService.deactivate(tenantId, id);
  }
}
