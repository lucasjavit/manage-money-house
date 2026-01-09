package com.managehouse.money.repository;

import com.managehouse.money.entity.ExtractExpenseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExtractExpenseTypeRepository extends JpaRepository<ExtractExpenseType, Long> {
    Optional<ExtractExpenseType> findByName(String name);
}

