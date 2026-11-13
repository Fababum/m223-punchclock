package ch.zli.m223.service;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import ch.zli.m223.model.Category;
import ch.zli.m223.model.Entry;
import ch.zli.m223.repository.CategoryRepository;

@ApplicationScoped
public class CategoryService {
    @Inject
    private CategoryRepository categoryRepository;

    @Transactional
    public Category createCategory(Category category) {
        categoryRepository.persist(category);
        return category;
    }

    @Transactional
    public boolean deleteCategory(Long id) {
        Category category = categoryRepository.findById(id);
        if (category == null) {
            return false;
        }
        if (category.getEntries() != null) {
            for (Entry entry : category.getEntries()) {
                entry.setCategory(null);
            }
        }
        categoryRepository.delete(category);
        return true;
    }

    @Transactional
    public Category updateCategory(Long id, Category category) {
        Category existing = categoryRepository.findById(id);
        if (existing == null) {
            return null;
        }

        existing.setCategoryName(category.getCategoryName());
        return existing;
    }

    public List<Category> findAll() {
        return categoryRepository.listAll();
    }
}
