package com.verygoodbank.tes.web.service;

import com.verygoodbank.tes.web.domain.TradeInput;
import com.verygoodbank.tes.web.mapper.TradeInputMapper;
import com.verygoodbank.tes.web.mapper.TradeOutputMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeEnrichmentService {

    private final TradeInputMapper tradeInputMapper;
    private final TradeOutputMapper tradeOutputMapper;

    public Flux<String> enrich(Flux<TradeInput> inputFlux) {
        return inputFlux
                .filter(TradeInput::isValid)
                .map(tradeInputMapper::mapToTradeOutput)
                .map(tradeOutputMapper::mapToCsv)
                .startWith(tradeOutputMapper.getCsvHeader())
                .doOnError(error -> log.error("Failed to enrich trade", error))
                .onErrorResume(e -> Flux.error(new Exception(e.getMessage())));
    }

}

