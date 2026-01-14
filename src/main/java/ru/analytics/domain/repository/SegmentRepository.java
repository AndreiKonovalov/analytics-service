package ru.analytics.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.analytics.domain.model.Segment;

public interface SegmentRepository extends JpaRepository<Segment, Long> {
}