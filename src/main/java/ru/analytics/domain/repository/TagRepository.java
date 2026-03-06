package ru.analytics.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.analytics.domain.model.Tag;

public interface TagRepository extends JpaRepository<Tag, Long> {
}