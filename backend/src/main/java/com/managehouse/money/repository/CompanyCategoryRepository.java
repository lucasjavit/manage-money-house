package com.managehouse.money.repository;

import com.managehouse.money.entity.CompanyCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyCategoryRepository extends JpaRepository<CompanyCategory, Long> {
    @Query("SELECT c FROM CompanyCategory c WHERE c.user.id = :userId ORDER BY c.type, c.name")
    List<CompanyCategory> findByUserId(@Param("userId") Long userId);
}
