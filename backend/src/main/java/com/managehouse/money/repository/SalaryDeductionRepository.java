package com.managehouse.money.repository;

import com.managehouse.money.entity.SalaryDeduction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalaryDeductionRepository extends JpaRepository<SalaryDeduction, Long> {
    @Query("SELECT s FROM SalaryDeduction s WHERE s.user.id = :userId AND s.month = :month AND s.year = :year ORDER BY s.dueDate ASC")
    List<SalaryDeduction> findByUserIdAndMonthAndYear(
        @Param("userId") Long userId,
        @Param("month") Integer month,
        @Param("year") Integer year
    );

    @Query("SELECT s FROM SalaryDeduction s WHERE s.user.id = :userId ORDER BY s.year DESC, s.month DESC, s.dueDate ASC")
    List<SalaryDeduction> findByUserIdOrderByYearMonthDesc(@Param("userId") Long userId);
}

