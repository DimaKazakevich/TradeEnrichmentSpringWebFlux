package com.verygoodbank.tes.web.controller;

import com.verygoodbank.tes.web.domain.TradeInput;
import com.verygoodbank.tes.web.mapper.TradeInputMapper;
import com.verygoodbank.tes.web.service.TradeEnrichmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("api/v1")
@RequiredArgsConstructor
@Slf4j
public class TradeEnrichmentController {

    private final TradeEnrichmentService tradeEnrichmentService;
    private final TradeInputMapper tradeInputMapper;

    @PostMapping(value = "/enrich", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<String>> enrichCsvData(@RequestBody Flux<DataBuffer> csvDataBufferFlux) {
        return ResponseEntity.ok(Flux.defer(() -> {
            Flux<TradeInput> tradeInputFlux = tradeInputMapper.mapToTradeInputFlux(csvDataBufferFlux);
            return tradeEnrichmentService.enrich(tradeInputFlux);
        }));
    }

}