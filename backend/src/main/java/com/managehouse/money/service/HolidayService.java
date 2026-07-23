package com.managehouse.money.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.managehouse.money.entity.Holiday;
import com.managehouse.money.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Feriados nacionais (BR) / federais (US) da API pública Nager.Date (sem chave).
 * Cache permanente em tabela: busca 1x por (ano, país) e persiste.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HolidayService {

    private static final String NAGER_URL = "https://date.nager.at/api/v3/publicholidays/%d/%s";

    private final HolidayRepository holidayRepository;
    private final WebClient.Builder webClientBuilder;

    /** DTO parcial da resposta da Nager. `global` distingue nacional/federal de estadual/regional. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NagerHoliday(String date, String localName, String name, Boolean global) {}

    /** Feriados do país no intervalo [start, end], garantindo o cache de todos os anos envolvidos. */
    public Set<LocalDate> getHolidayDates(String country, LocalDate start, LocalDate end) {
        String c = country == null ? "BR" : country.trim().toUpperCase();
        for (int year = start.getYear(); year <= end.getYear(); year++) {
            ensureCached(c, year);
        }
        return holidayRepository.findByCountryAndDateBetween(c, start, end).stream()
                .map(Holiday::getDate)
                .collect(Collectors.toSet());
    }

    /** Garante que os feriados de (país, ano) estejam no banco; busca na Nager se faltar. */
    private synchronized void ensureCached(String country, int year) {
        if (holidayRepository.existsByCountryAndYear(country, year)) {
            return;
        }
        try {
            String url = String.format(NAGER_URL, year, country);
            List<NagerHoliday> holidays = webClientBuilder.build()
                    .get().uri(url)
                    .retrieve()
                    .bodyToFlux(NagerHoliday.class)
                    .collectList()
                    .block(Duration.ofSeconds(10));

            if (holidays == null) {
                log.warn("Nager retornou vazio para {} {}", country, year);
                return;
            }
            // Só nacionais (BR) / federais (US): global == true.
            List<Holiday> toSave = holidays.stream()
                    .filter(h -> Boolean.TRUE.equals(h.global()))
                    .map(h -> {
                        Holiday e = new Holiday();
                        e.setCountry(country);
                        e.setDate(LocalDate.parse(h.date()));
                        e.setName(h.name() != null ? h.name() : h.localName());
                        e.setYear(year);
                        return e;
                    })
                    .collect(Collectors.toList());
            holidayRepository.saveAll(toSave);
            log.info("Feriados cacheados: {} {} -> {} nacionais", country, year, toSave.size());
        } catch (Exception e) {
            // Sem feriados => o cálculo cai para seg-sex puro. Não quebra a feature.
            log.warn("Falha ao buscar feriados {} {}: {}", country, year, e.getMessage());
        }
    }
}
