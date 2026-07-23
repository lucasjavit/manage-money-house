package com.managehouse.money.service;

import com.managehouse.money.dto.*;
import com.managehouse.money.entity.PtoConfig;
import com.managehouse.money.entity.PtoVacation;
import com.managehouse.money.entity.User;
import com.managehouse.money.repository.PtoConfigRepository;
import com.managehouse.money.repository.PtoVacationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controle de PTO do Lucas (Aditi): 1 dia a cada 25 dias corridos desde a data-base,
 * menos os dias úteis de férias (seg-sex, excluindo feriados do país escolhido).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PtoService {

    // 200h / 8h ao dia = 25 dias corridos por dia de PTO.
    private static final BigDecimal DAYS_PER_PTO = new BigDecimal("25");
    private static final int SCALE = 4;

    private final PtoConfigRepository configRepository;
    private final PtoVacationRepository vacationRepository;
    private final HolidayService holidayService;
    private final UserService userService;

    // ---- Config (upsert, 1 por usuário) ----

    @Transactional
    public PtoConfigResponse createOrUpdateConfig(PtoConfigRequest request) {
        User user = userService.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        PtoConfig config = configRepository.findByUserId(user.getId()).orElse(new PtoConfig());
        config.setUser(user);
        config.setBaseDate(request.getBaseDate());
        config.setInitialBalance(request.getInitialBalance());
        config.setCountry(normalizeCountry(request.getCountry()));

        return toConfigResponse(configRepository.save(config));
    }

    public PtoConfigResponse getConfig(Long userId) {
        return configRepository.findByUserId(userId).map(this::toConfigResponse).orElse(null);
    }

    // ---- Férias ----

    @Transactional
    public PtoVacationResponse createVacation(PtoVacationRequest request) {
        User user = userService.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        if (request.getStartDate() == null || request.getEndDate() == null
                || request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("Período de férias inválido");
        }

        PtoVacation v = new PtoVacation();
        v.setUser(user);
        v.setStartDate(request.getStartDate());
        v.setEndDate(request.getEndDate());
        v.setDescription(request.getDescription());
        v = vacationRepository.save(v);

        String country = configRepository.findByUserId(user.getId())
                .map(PtoConfig::getCountry).orElse("BR");
        return toVacationResponse(v, country);
    }

    public List<PtoVacationResponse> getVacations(Long userId) {
        String country = configRepository.findByUserId(userId)
                .map(PtoConfig::getCountry).orElse("BR");
        return vacationRepository.findByUserIdOrderByStartDate(userId).stream()
                .map(v -> toVacationResponse(v, country))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteVacation(Long id) {
        vacationRepository.deleteById(id);
    }

    // ---- Saldo / projeção ----

    public PtoBalanceResponse getBalance(Long userId, LocalDate refDate) {
        PtoConfig config = configRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Configure o PTO primeiro"));

        LocalDate ref = refDate != null ? refDate : LocalDate.now();
        String country = config.getCountry();

        // Acúmulo: 1/25 de dia por dia corrido desde a baseDate (nunca negativo).
        long diasCorridos = Math.max(0, ChronoUnit.DAYS.between(config.getBaseDate(), ref));
        BigDecimal accrued = new BigDecimal(diasCorridos)
                .divide(DAYS_PER_PTO, SCALE, RoundingMode.HALF_UP);

        // Férias usadas: período conta inteiro se ref >= startDate.
        BigDecimal used = BigDecimal.ZERO;
        for (PtoVacation v : vacationRepository.findByUserIdOrderByStartDate(userId)) {
            if (!ref.isBefore(v.getStartDate())) {
                used = used.add(new BigDecimal(countBusinessDays(v.getStartDate(), v.getEndDate(), country)));
            }
        }

        BigDecimal balance = config.getInitialBalance().add(accrued).subtract(used)
                .setScale(SCALE, RoundingMode.HALF_UP);

        long diasNoCiclo = diasCorridos % 25;
        int daysToNextPto = (int) (25 - diasNoCiclo);
        BigDecimal fraction = new BigDecimal(diasNoCiclo)
                .divide(DAYS_PER_PTO, SCALE, RoundingMode.HALF_UP);

        return new PtoBalanceResponse(ref, balance, config.getInitialBalance(),
                accrued, used, daysToNextPto, fraction, country);
    }

    /** Dias úteis (seg-sex) no intervalo, excluindo feriados do país. */
    public int countBusinessDays(LocalDate start, LocalDate end, String country) {
        Set<LocalDate> holidays = holidayService.getHolidayDates(country, start, end);
        int count = 0;
        LocalDate cur = start;
        while (!cur.isAfter(end)) {
            DayOfWeek dow = cur.getDayOfWeek();
            boolean weekday = dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
            if (weekday && !holidays.contains(cur)) {
                count++;
            }
            cur = cur.plusDays(1);
        }
        return count;
    }

    // ---- helpers ----

    private String normalizeCountry(String c) {
        String up = c == null ? "BR" : c.trim().toUpperCase();
        return up.equals("US") ? "US" : "BR";
    }

    private PtoConfigResponse toConfigResponse(PtoConfig c) {
        return new PtoConfigResponse(c.getId(), c.getUser().getId(), c.getBaseDate(),
                c.getInitialBalance(), c.getCountry(), c.getCreatedAt(), c.getUpdatedAt());
    }

    private PtoVacationResponse toVacationResponse(PtoVacation v, String country) {
        int businessDays = countBusinessDays(v.getStartDate(), v.getEndDate(), country);
        return new PtoVacationResponse(v.getId(), v.getUser().getId(), v.getStartDate(),
                v.getEndDate(), v.getDescription(), businessDays, v.getCreatedAt());
    }
}
