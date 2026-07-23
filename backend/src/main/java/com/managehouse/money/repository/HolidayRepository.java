package com.managehouse.money.repository;

import com.managehouse.money.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    boolean existsByCountryAndYear(String country, Integer year);

    List<Holiday> findByCountryAndDateBetween(String country, LocalDate start, LocalDate end);
}
