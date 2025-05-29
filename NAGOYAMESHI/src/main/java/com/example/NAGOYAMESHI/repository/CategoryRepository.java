package com.example.NAGOYAMESHI.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.NAGOYAMESHI.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Integer>{
    boolean existsByName(String name);
    
    public Page<Category> findByNameLike(String keyword, Pageable pageable);

    
}
