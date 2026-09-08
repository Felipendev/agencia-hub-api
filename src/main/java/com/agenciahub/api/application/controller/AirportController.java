package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.persistence.entity.Airport;
import com.agenciahub.api.application.persistence.repository.AirportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

/** Autocomplete de cidade/aeroporto (nossa base própria — ver migração V39). Qualquer usuário autenticado pode buscar. */
@RestController @RequestMapping("/airports") @RequiredArgsConstructor
public class AirportController {
 private static final int CANDIDATE_LIMIT = 40;
 private static final int RESULT_LIMIT = 10;
 private final AirportRepository airports;

 @GetMapping public List<AirportSuggestion> search(@RequestParam(required = false) String q) {
  if (q == null || q.strip().length() < 2) return List.of();
  String term = q.strip();
  List<Airport> candidates = airports.search(term, PageRequest.of(0, CANDIDATE_LIMIT));
  return candidates.stream()
   .sorted(Comparator.comparingInt((Airport a) -> relevance(a, term)).thenComparing(Airport::getCity))
   .limit(RESULT_LIMIT)
   .map(AirportSuggestion::from)
   .toList();
 }

 /** Menor = mais relevante: sigla IATA exata, depois começa com o termo (cidade/aeroporto), depois contém em algum lugar. */
 private static int relevance(Airport a, String term) {
  String t = term.toLowerCase();
  if (a.getIataCode().equalsIgnoreCase(term)) return 0;
  if (a.getCity().toLowerCase().startsWith(t)) return 1;
  if (a.getName().toLowerCase().startsWith(t)) return 2;
  if (a.getIataCode().toLowerCase().startsWith(t)) return 3;
  return 4;
 }

 public record AirportSuggestion(String iata, String city, String name, String country, String label) {
  static AirportSuggestion from(Airport a) {
   return new AirportSuggestion(a.getIataCode(), a.getCity(), a.getName(), a.getCountry(), a.getCity() + " (" + a.getIataCode() + ")");
  }
 }
}
