package com.managehouse.money.repository;

import com.managehouse.money.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByYear(Integer year);
    
    @Query("SELECT e FROM Expense e WHERE e.year = :year AND e.month = :month AND e.expenseType.id = :expenseTypeId AND e.user.id = :userId")
    Optional<Expense> findByYearAndMonthAndExpenseTypeAndUser(
        @Param("year") Integer year,
        @Param("month") Integer month,
        @Param("expenseTypeId") Long expenseTypeId,
        @Param("userId") Long userId
    );
    
    @Query("SELECT e FROM Expense e WHERE e.year = :year AND e.month = :month AND e.expenseType.id = :expenseTypeId")
    Optional<Expense> findByYearAndMonthAndExpenseType(
        @Param("year") Integer year,
        @Param("month") Integer month,
        @Param("expenseTypeId") Long expenseTypeId
    );
    
    @Query("SELECT e FROM Expense e WHERE e.year = :year AND e.month = :month")
    List<Expense> findByYearAndMonth(@Param("year") Integer year, @Param("month") Integer month);
}

