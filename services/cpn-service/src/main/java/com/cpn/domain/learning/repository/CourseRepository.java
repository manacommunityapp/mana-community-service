package com.cpn.domain.learning.repository;

import com.cpn.domain.learning.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {
    List<Course> findByCategory(String category);
}
