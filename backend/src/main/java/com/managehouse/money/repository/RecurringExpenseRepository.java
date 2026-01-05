package com.managehouse.money.repository;

import com.managehouse.money.entity.RecurringExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecurringExpenseRepository extends JpaRepository<RecurringExpense, Long> {
    List<RecurringExpense> findByUserId(Long userId);

    @Query("SELECT COUNT(r) FROM RecurringExpense r WHERE r.expenseType.id = :expenseTypeId")
    long countByExpenseTypeId(@Param("expenseTypeId") Long expenseTypeId);
}

