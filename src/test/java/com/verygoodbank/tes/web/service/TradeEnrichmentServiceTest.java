package com.verygoodbank.tes.web.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TradeEnrichmentServiceTest {

    private TradeEnrichmentService tradeEnrichmentService;
    ClassPathResource productFileClassPath = new ClassPathResource("/product.csv");

    @BeforeEach
    public void setUp() {
        tradeEnrichmentService = new TradeEnrichmentService();
    }

    @AfterEach
    public void setUp1() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(productFileClassPath.getFile(), false))) {
            writer.write("product_id,product_name\n");
            writer.write("1,Treasury Bills Domestic\n");
            writer.write("2,Corporate Bonds Domestic\n");
            writer.write("3,REPO Domestic\n");
            writer.write("4,Interest rate swaps International\n");
            writer.write("5,OTC Index Option\n");
            writer.write("6,Currency Options\n");
            writer.write("7,Reverse Repos International\n");
            writer.write("8,REPO International\n");
            writer.write("9,766A_CORP BD\n");
            writer.write("10,766B_CORP BD\n");
        }
    }

    @Test
    public void testEnrichValidCsv() {
        String csvContent = "date,product_id,currency,price\n" +
                "20160101,1,EUR,10.0\n" +
                "20160101,2,USD,20.1\n";

        Flux<String> result = tradeEnrichmentService.enrich(csvContent);

        StepVerifier.create(result)
                .expectNext("date,product_name,currency,price\n")
                .expectNext("20160101,Treasury Bills Domestic,EUR,10.0\n")
                .expectNext("20160101,Corporate Bonds Domestic,USD,20.1\n")
                .verifyComplete();
    }

    @Test
    public void testEnrichInvalidDateCsv() {
        String csvContent = "date,product_id,currency,price\n" +
                "invalidDate,1,EUR,10.0\n";

        Flux<String> result = tradeEnrichmentService.enrich(csvContent);

        StepVerifier.create(result)
                .expectNext("date,product_name,currency,price\n")
                .verifyComplete();
    }

    @Test
    public void testMissingProductMapping() {
        String csvContent = "date,product_id,currency,price\n" +
                "20160101,999,EUR,10.0\n";

        Flux<String> result = tradeEnrichmentService.enrich(csvContent);

        StepVerifier.create(result)
                .expectNext("date,product_name,currency,price\n")
                .expectNext("20160101,Missing Product Name,EUR,10.0\n")
                .verifyComplete();
    }

    @Test
    public void testEmptyCsv() {
        String csvContent = "date,product_id,currency,price\n";

        Flux<String> result = tradeEnrichmentService.enrich(csvContent);

        StepVerifier.create(result)
                .expectNext("date,product_name,currency,price\n")
                .verifyComplete();
    }

    @Test
    public void testProductFileLimitExceeded() throws IOException {

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(productFileClassPath.getFile()))) {
            writer.write("product_id,product_name\n");
            for (int i = 0; i < 100_002; i++) {
                writer.write(i + ",Product " + i + "\n");
            }
        }

        Exception thrown = assertThrows(
                RuntimeException.class,
                TradeEnrichmentService::new,
                "Expected TradeEnrichmentService to throw RuntimeException"
        );

        assertTrue(thrown.getMessage().contains("Product.csv has more than 100_000 products"));
    }

    @Test
    public void testStressEnrichWithLargeData() {
        StringBuilder csvContent = new StringBuilder("date,product_id,currency,price\n");

        for (int i = 1; i <= 1_000_000; i++) {
            int productId = (i % 3) + 1;
            csvContent.append(String.format("20160101,%d,EUR,%.2f\n", productId, 10.0 + i));
        }

        Flux<String> result = tradeEnrichmentService.enrich(csvContent.toString());

        StepVerifier.create(result)
                .expectNext("date,product_name,currency,price\n")
                .expectNextCount(1_000_000)
                .verifyComplete();
    }
}

