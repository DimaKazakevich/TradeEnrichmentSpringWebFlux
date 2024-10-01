package com.verygoodbank.tes.web.controller;

import com.verygoodbank.tes.web.service.TradeEnrichmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("api/v1")
@RequiredArgsConstructor
public class TradeEnrichmentController {

    private final TradeEnrichmentService tradeEnrichmentService;

    @PostMapping(value = "/enrich", consumes = "text/csv", produces = "text/csv")
    public ResponseEntity<Flux<String>> enrich(@RequestBody String csvTradeContent) {
        Flux<String> enrichedTradeFlux = tradeEnrichmentService.enrich(csvTradeContent);

        return ResponseEntity.ok()
                .body(enrichedTradeFlux);
    }

}


