package com.verygoodbank.tes.web.service;

import com.verygoodbank.tes.web.domain.TradeInput;
import com.verygoodbank.tes.web.domain.TradeOutput;
import com.verygoodbank.tes.web.mapper.TradeInputMapper;
import com.verygoodbank.tes.web.mapper.TradeOutputMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

public class TradeEnrichmentServiceTest {

    private TradeEnrichmentService tradeEnrichmentService;

    @BeforeEach
    public void setUp() {
        Map<String, String> products = new HashMap<>();
        products.put("1", "Treasury Bills Domestic");
        products.put("2", "Corporate Bonds Domestic");

        TradeOutputMapper tradeOutputMapper = mock(TradeOutputMapper.class);
        TradeInputMapper tradeInputMapper = new TradeInputMapper(products);

        tradeEnrichmentService = new TradeEnrichmentService(tradeInputMapper, tradeOutputMapper);

        when(tradeOutputMapper.mapToCsv(any(TradeOutput.class)))
                .thenAnswer(invocation -> {
                    TradeOutput tradeOutput = invocation.getArgument(0);
                    return tradeOutput.getDate() + "," + tradeOutput.getProductName() + "," + tradeOutput.getCurrency() + "," + tradeOutput.getPrice();
                });

        when(tradeOutputMapper.getCsvHeader())
                .thenReturn("date,product_name,currency,price");
    }

    @Test
    public void testEnrichValidInput() {
        Flux<TradeInput> inputFlux = Flux.just(
                TradeInput.builder().date("20160101").productId("1").currency("EUR").price(new BigDecimal("10.0")).build(),
                TradeInput.builder().date("20160101").productId("2").currency("USD").price(new BigDecimal("20.1")).build()
        );

        Flux<String> resultFlux = tradeEnrichmentService.enrich(inputFlux);

        StepVerifier.create(resultFlux)
                    .expectNext("date,product_name,currency,price")
                    .expectNext("20160101,Treasury Bills Domestic,EUR,10.0")
                    .expectNext("20160101,Corporate Bonds Domestic,USD,20.1")
                    .verifyComplete();
    }

    @Test
    public void testEnrichWithInvalidProductId() {
        Flux<TradeInput> inputFlux = Flux.just(
                TradeInput.builder().date("20160101").productId("999").currency("EUR").price(new BigDecimal("10.0")).build()
        );

        Flux<String> resultFlux = tradeEnrichmentService.enrich(inputFlux);

        StepVerifier.create(resultFlux)
                    .expectNext("date,product_name,currency,price")
                    .expectNext("20160101,Missing Product Name,EUR,10.0")
                    .verifyComplete();
    }

    @Test
    public void testEnrichWithInvalidTradeInput() {
        Flux<TradeInput> inputFlux = Flux.just(
                TradeInput.builder().date("").productId("1").currency("EUR").price(new BigDecimal("10.0")).build()
        );

        Flux<String> resultFlux = tradeEnrichmentService.enrich(inputFlux);

        StepVerifier.create(resultFlux)
                    .expectNext("date,product_name,currency,price")
                    .verifyComplete();
    }
}
