package ru.analytics.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.analytics.domain.model.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}