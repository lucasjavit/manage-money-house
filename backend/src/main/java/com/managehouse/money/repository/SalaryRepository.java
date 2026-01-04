package com.managehouse.money.repository;

import com.managehouse.money.entity.Salary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SalaryRepository extends JpaRepository<Salary, Long> {
    @Query("SELECT s FROM Salary s WHERE s.user.id = :userId")
    Optional<Salary> findByUserId(@Param("userId") Long userId);
}

