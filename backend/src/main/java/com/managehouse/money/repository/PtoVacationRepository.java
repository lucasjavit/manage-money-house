package com.managehouse.money.repository;

import com.managehouse.money.entity.PtoVacation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PtoVacationRepository extends JpaRepository<PtoVacation, Long> {

    @Query("SELECT v FROM PtoVacation v WHERE v.user.id = :userId ORDER BY v.startDate ASC")
    List<PtoVacation> findByUserIdOrderByStartDate(@Param("userId") Long userId);
}
