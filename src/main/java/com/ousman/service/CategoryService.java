package com.ousman.service;

import com.ousman.model.Category;
import com.ousman.repository.CategoryRepository;
import com.ousman.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<Category> getAll(String search) {
        return categoryRepository.search(search);
    }

    @Transactional(readOnly = true)
    public List<Category> getActive() {
        return categoryRepository.findAllActive();
    }

    @Transactional(readOnly = true)
    public Category getById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found: " + id));
    }

    public Category create(Category req) {
        if (req.getName() == null || req.getName().isBlank()) {
            throw new RuntimeException("Category name is required.");
        }
        String name = req.getName().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new RuntimeException("A category named '" + name + "' already exists.");
        }
        req.setName(name);
        if (req.getActive() == null) req.setActive(true);
        return categoryRepository.saveAndFlush(req);
    }

    public Category update(Long id, Category req) {
        Category existing = getById(id);
        if (req.getName() != null && !req.getName().isBlank()) {
            String newName = req.getName().trim();
            categoryRepository.findByNameIgnoreCase(newName).ifPresent(other -> {
                if (!other.getId().equals(id)) {
                    throw new RuntimeException("A category named '" + newName + "' already exists.");
                }
            });
            existing.setName(newName);
        }
        existing.setDescription(req.getDescription());
        if (req.getActive() != null) existing.setActive(req.getActive());
        return categoryRepository.saveAndFlush(existing);
    }

    /** Only safe to delete a category with no products assigned — otherwise deactivate. */
    public void delete(Long id) {
        Category category = getById(id);
        if (!productRepository.findByCategoryId(id).isEmpty()) {
            throw new RuntimeException("This category has products assigned to it and cannot be deleted. Deactivate it instead.");
        }
        categoryRepository.delete(category);
    }
}
