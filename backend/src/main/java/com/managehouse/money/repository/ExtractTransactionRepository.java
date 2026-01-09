package com.managehouse.money.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.managehouse.money.entity.ExtractTransaction;

@Repository
public interface ExtractTransactionRepository extends JpaRepository<ExtractTransaction, Long> {
    @Query("SELECT e FROM ExtractTransaction e WHERE e.user.id = :userId ORDER BY e.transactionDate DESC")
    List<ExtractTransaction> findByUserIdOrderByTransactionDateDesc(@Param("userId") Long userId);
    
    @Query("SELECT e FROM ExtractTransaction e WHERE e.user.id = :userId AND e.transactionDate BETWEEN :startDate AND :endDate ORDER BY e.transactionDate DESC")
    List<ExtractTransaction> findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(
        @Param("userId") Long userId, 
        @Param("startDate") java.time.LocalDate startDate, 
        @Param("endDate") java.time.LocalDate endDate
    );

    @Query("SELECT COUNT(e) FROM ExtractTransaction e WHERE e.extractExpenseType.id = :expenseTypeId")
    long countByExpenseTypeId(@Param("expenseTypeId") Long expenseTypeId);
}

