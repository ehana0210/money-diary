package com.moneydiary.api.web;

import com.moneydiary.api.domain.Category;
import com.moneydiary.api.repository.CategoryRepository;
import com.moneydiary.api.security.FirebaseUser;
import com.moneydiary.api.web.dto.CategoryRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryRepository repository;

    public CategoryController(CategoryRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Category> list(@AuthenticationPrincipal FirebaseUser user) {
        return repository.findAll(user.uid());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Category create(@AuthenticationPrincipal FirebaseUser user,
                           @RequestBody CategoryRequest request) {
        Category category = toEntity(request);
        category.setCreatedAt(System.currentTimeMillis());
        return repository.create(user.uid(), category);
    }

    @PutMapping("/{id}")
    public Category update(@AuthenticationPrincipal FirebaseUser user,
                           @PathVariable String id,
                           @RequestBody CategoryRequest request) {
        Category existing = repository.findAll(user.uid()).stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElse(null);
        Category category = toEntity(request);
        category.setCreatedAt(existing != null ? existing.getCreatedAt() : System.currentTimeMillis());
        return repository.update(user.uid(), id, category);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal FirebaseUser user, @PathVariable String id) {
        repository.delete(user.uid(), id);
    }

    private Category toEntity(CategoryRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("name 은 필수입니다.");
        }
        Category category = new Category();
        category.setName(request.name());
        category.setType(request.type());
        category.setIcon(request.icon());
        category.setCustom(request.custom() != null && request.custom());
        return category;
    }
}
