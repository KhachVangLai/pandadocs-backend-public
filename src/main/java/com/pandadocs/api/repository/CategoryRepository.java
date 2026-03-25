package com.pandadocs.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.pandadocs.api.model.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
}