package com.verygoodbank.tes.web.controller;

import com.verygoodbank.tes.web.service.TradeEnrichmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = TradeEnrichmentController.class)
public class TradeEnrichmentControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private TradeEnrichmentService tradeEnrichmentService;
    
    private static final String TEXT_CSV_CONTENT_TYPE = "text/csv";
    private static final String TEXT_CSV_CONTENT_TYPE_WITH_CHARSET = "text/csv;charset=UTF-8";
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
        when(tradeEnrichmentService.enrich(anyString()))
                .thenReturn(Flux.just(
                        "date,product_name,currency,price\n",
                        "20160101,Treasury Bills Domestic,EUR,10.0\n",
                        "20160101,Corporate Bonds Domestic,USD,20.1\n"));

        webTestClient.post()
                .uri(ENDPOINT_TO_TEST)
                .contentType(MediaType.parseMediaType(TEXT_CSV_CONTENT_TYPE))
                .accept(MediaType.parseMediaType(TEXT_CSV_CONTENT_TYPE))
                .bodyValue(validCsvContent)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(TEXT_CSV_CONTENT_TYPE_WITH_CHARSET)
                .expectBody(String.class)
                .consumeWith(response -> {
                    String responseBody = response.getResponseBody();
                    assert responseBody != null;
                    StepVerifier.create(Flux.just(responseBody.split("\n")))
                            .expectNext("date,product_name,currency,price")
                            .expectNext("20160101,Treasury Bills Domestic,EUR,10.0")
                            .expectNext("20160101,Corporate Bonds Domestic,USD,20.1")
                            .verifyComplete();
                });
    }

    @Test
    public void testEnrichInvalidCsv() {
        String invalidCsvContent = "date,product_id,currency,price\ninvalidDate,1,EUR,10.0\n";

        when(tradeEnrichmentService.enrich(anyString()))
                .thenReturn(Flux.just("date,product_name,currency,price\n"));

        webTestClient.post()
                .uri(ENDPOINT_TO_TEST)
                .contentType(MediaType.parseMediaType(TEXT_CSV_CONTENT_TYPE))
                .accept(MediaType.parseMediaType(TEXT_CSV_CONTENT_TYPE))
                .bodyValue(invalidCsvContent)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(TEXT_CSV_CONTENT_TYPE_WITH_CHARSET)
                .expectBody(String.class)
                .consumeWith(response -> {
                    String responseBody = response.getResponseBody();
                    assert responseBody != null;
                    StepVerifier.create(Flux.just(responseBody.split("\n")))
                            .expectNext("date,product_name,currency,price")
                            .verifyComplete();
                });
    }

    @Test
    public void testEnrichStreamResponse() {
        when(tradeEnrichmentService.enrich(anyString()))
                .thenReturn(Flux.just(
                                "date,product_name,currency,price\n",
                                "20160101,Treasury Bills Domestic,EUR,10.0\n",
                                "20160101,Corporate Bonds Domestic,USD,20.1\n")
                        );

        webTestClient.post()
                .uri(ENDPOINT_TO_TEST)
                .contentType(MediaType.parseMediaType(TEXT_CSV_CONTENT_TYPE))
                .accept(MediaType.parseMediaType(TEXT_CSV_CONTENT_TYPE))
                .bodyValue(validCsvContent)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(TEXT_CSV_CONTENT_TYPE_WITH_CHARSET)
                .returnResult(String.class)
                .getResponseBody()
                .as(StepVerifier::create)
                .expectNext("date,product_name,currency,price")
                .expectNext("20160101,Treasury Bills Domestic,EUR,10.0")
                .expectNext("20160101,Corporate Bonds Domestic,USD,20.1")
                .verifyComplete();
    }
}

