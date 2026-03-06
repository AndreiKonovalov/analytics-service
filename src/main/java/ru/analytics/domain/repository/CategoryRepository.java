package ru.analytics.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.analytics.domain.model.Category;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);

}