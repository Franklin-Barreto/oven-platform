package br.com.f2e.ovenplatform.catalog.application;

import br.com.f2e.ovenplatform.catalog.domain.Category;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {

  private static final String RESOURCE = "Category";

  private final CategoryRepository categoryRepository;

  public CategoryService(CategoryRepository categoryRepository) {
    this.categoryRepository = categoryRepository;
  }

  public Category save(UUID tenantId, String name) {
    return categoryRepository.save(new Category(name, tenantId));
  }

  public Optional<Category> findCategory(UUID tenantId, UUID categoryId) {
    return categoryRepository.findByIdAndTenantId(categoryId, tenantId);
  }

  public List<Category> listCategories(UUID tenantId) {
    return categoryRepository.findByTenantId(tenantId);
  }

  public Category update(UUID tenantId, UUID categoryId, String name, boolean active) {
    var category = findRequiredCategory(tenantId, categoryId);
    category.rename(name);
    if (active) {
      category.activate();
    } else {
      category.deactivate();
    }
    return categoryRepository.save(category);
  }

  public void deactivate(UUID tenantId, UUID categoryId) {
    var category = findRequiredCategory(tenantId, categoryId);
    category.deactivate();
    categoryRepository.save(category);
  }

  private Category findRequiredCategory(UUID tenantId, UUID categoryId) {
    return categoryRepository
        .findByIdAndTenantId(categoryId, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, categoryId));
  }
}
