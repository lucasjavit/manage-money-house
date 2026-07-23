package com.managehouse.money.repository;

import com.managehouse.money.entity.PtoConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PtoConfigRepository extends JpaRepository<PtoConfig, Long> {
    Optional<PtoConfig> findByUserId(Long userId);
}
