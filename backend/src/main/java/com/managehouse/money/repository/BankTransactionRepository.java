package com.managehouse.money.repository;

import com.managehouse.money.entity.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {

    Optional<BankTransaction> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);

    @Query("SELECT b FROM BankTransaction b WHERE b.user.id = :userId " +
           "AND b.transactionDate BETWEEN :start AND :end ORDER BY b.transactionDate DESC, b.id DESC")
    List<BankTransaction> findByUserAndPeriod(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Query("SELECT b FROM BankTransaction b WHERE b.user.id = :userId " +
           "AND EXTRACT(YEAR FROM b.transactionDate) = :year " +
           "AND EXTRACT(MONTH FROM b.transactionDate) = :month " +
           "ORDER BY b.needsReview DESC, b.transactionDate DESC, b.id DESC")
    List<BankTransaction> findByUserAndMonth(
            @Param("userId") Long userId,
            @Param("year") Integer year,
            @Param("month") Integer month);
}
