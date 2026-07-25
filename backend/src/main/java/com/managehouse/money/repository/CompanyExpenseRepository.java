package com.managehouse.money.repository;

import com.managehouse.money.entity.CompanyExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyExpenseRepository extends JpaRepository<CompanyExpense, Long> {
    @Query("SELECT e FROM CompanyExpense e WHERE e.user.id = :userId AND e.year = :year AND e.month = :month " +
           "ORDER BY e.dueDate ASC, e.id ASC")
    List<CompanyExpense> findByUserAndMonth(@Param("userId") Long userId,
                                            @Param("year") Integer year,
                                            @Param("month") Integer month);

    long countByCategoryId(Long categoryId);
}
