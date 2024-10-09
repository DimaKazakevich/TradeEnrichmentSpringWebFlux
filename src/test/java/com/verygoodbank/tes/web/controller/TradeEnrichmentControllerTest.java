package com.verygoodbank.tes.web.controller;

import com.verygoodbank.tes.web.domain.TradeInput;
import com.verygoodbank.tes.web.mapper.TradeInputMapper;
import com.verygoodbank.tes.web.service.TradeEnrichmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

@WebFluxTest(controllers = TradeEnrichmentController.class)
public class TradeEnrichmentControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private TradeEnrichmentService tradeEnrichmentService;

    @MockBean
    private TradeInputMapper tradeInputMapper;

    private static final String ENDPOINT_TO_TEST = "/api/v1/enrich";

    private String validCsvContent;

    @BeforeEach
    public void setUp() {
        validCsvContent = "date,product_id,currency,price\n" +
                "20160101,1,EUR,10.0\n" +
                "20160101,2,USD,20.1\n";
    }

    @Test
    public void testEnrichValidCsv() {
        Flux<TradeInput> inputFlux = Flux.just(
                TradeInput.builder().date("20160101").productId("1").currency("EUR").price(new BigDecimal("10.0"))
                          .build(),
                TradeInput.builder().date("20160101").productId("2").currency("USD").price(new BigDecimal("20.1"))
                          .build()
        );

        Mockito.when(tradeInputMapper.mapToTradeInputFlux(Mockito.any(Flux.class)))
               .thenReturn(inputFlux);

        Flux<String> enrichedData = Flux.just(
                "date,product_name,currency,price",
                "20160101,Treasury Bills Domestic,EUR,10.0",
                "20160101,Corporate Bonds Domestic,EUR,20.1");
        Mockito.when(tradeEnrichmentService.enrich(Mockito.any(Flux.class)))
               .thenReturn(enrichedData);

        webTestClient.post()
                     .uri(ENDPOINT_TO_TEST)
                     .contentType(MediaType.TEXT_PLAIN)
                     .body(BodyInserters.fromValue(validCsvContent))
                     .exchange()
                     .expectStatus().isOk()
                     .returnResult(String.class)
                     .getResponseBody()
                     .as(StepVerifier::create)
                     .expectNext("date,product_name,currency,price")
                     .expectNext("20160101,Treasury Bills Domestic,EUR,10.0")
                     .expectNext("20160101,Corporate Bonds Domestic,EUR,20.1")
                     .verifyComplete();
    }

    @Test
    public void testEnrichInvalidCsv() {
        String invalidCsvContent = "date,product_id,currency,price\ninvalidDate,1,EUR,10.0\n";

        Flux<TradeInput> inputFlux = Flux.just(
                TradeInput.builder().date("invalidDate").productId("1").currency("EUR").price(new BigDecimal("10.0"))
                          .build()
        );

        Mockito.when(tradeInputMapper.mapToTradeInputFlux(Mockito.any(Flux.class)))
               .thenReturn(inputFlux);

        Flux<String> enrichedData = Flux.just(
                "date,product_name,currency,price");
        Mockito.when(tradeEnrichmentService.enrich(Mockito.any(Flux.class)))
               .thenReturn(enrichedData);

        webTestClient.post()
                     .uri(ENDPOINT_TO_TEST)
                     .contentType(MediaType.TEXT_PLAIN)
                     .body(BodyInserters.fromValue(invalidCsvContent))
                     .exchange()
                     .expectStatus().isOk()
                     .returnResult(String.class)
                     .getResponseBody()
                     .as(StepVerifier::create)
                     .expectNext("date,product_name,currency,price")
                     .verifyComplete();
    }

    @Test
    public void stressTestWithLargeCsv() {
        StringBuilder largeCsvContent = new StringBuilder("date,product_id,currency,price\n");
        for (int i = 0; i < 1_000_000; i++) {
            largeCsvContent.append("20160101,")
                           .append(i)
                           .append(",EUR,")
                           .append(10.0 + i)
                           .append("\n");
        }

        Mockito.when(tradeInputMapper.mapToTradeInputFlux(Mockito.any(Flux.class)))
               .thenReturn(Flux.range(0, 1_000_000)
                               .map(i -> TradeInput.builder()
                                                   .date("20160101")
                                                   .productId(String.valueOf(i))
                                                   .currency("EUR")
                                                   .price(new BigDecimal(10.0 + i))
                                                   .build()));

        Flux<String> enrichedData = Flux.range(0, 1_000_000)
                                        .map(i -> "20160101,Product " + i + ",EUR," + (10.0 + i));

        Mockito.when(tradeEnrichmentService.enrich(Mockito.any(Flux.class)))
               .thenReturn(enrichedData);

        webTestClient.post()
                     .uri(ENDPOINT_TO_TEST)
                     .contentType(MediaType.TEXT_PLAIN)
                     .body(BodyInserters.fromValue(largeCsvContent.toString()))
                     .exchange()
                     .expectStatus().isOk()
                     .returnResult(String.class)
                     .getResponseBody()
                     .as(StepVerifier::create)
                     .expectNextCount(1_000_000)
                     .verifyComplete();
    }

}

