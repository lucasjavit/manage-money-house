package com.managehouse.money.repository;

import com.managehouse.money.entity.SalaryConversion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SalaryConversionRepository extends JpaRepository<SalaryConversion, Long> {
    @Query("SELECT sc FROM SalaryConversion sc WHERE sc.user.id = :userId AND sc.month = :month AND sc.year = :year")
    Optional<SalaryConversion> findByUserIdAndMonthAndYear(
        @Param("userId") Long userId,
        @Param("month") Integer month,
        @Param("year") Integer year
    );
}

