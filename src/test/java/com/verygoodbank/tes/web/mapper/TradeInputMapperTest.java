package com.verygoodbank.tes.web.mapper;

import com.verygoodbank.tes.web.domain.TradeInput;
import com.verygoodbank.tes.web.domain.TradeOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
public class TradeInputMapperTest {

    private TradeInputMapper tradeInputMapper;

    @BeforeEach
    public void setUp() {
        Map<String, String> products = new HashMap<>();
        products.put("1", "Treasury Bills Domestic");
        products.put("2", "Corporate Bonds Domestic");
        tradeInputMapper = new TradeInputMapper(products);
    }

    @Test
    public void testMapToTradeOutput() {
        TradeInput tradeInput = TradeInput.builder()
                                          .date("20160101")
                                          .productId("1")
                                          .currency("EUR")
                                          .price(new BigDecimal("10.0"))
                                          .build();

        TradeOutput tradeOutput = tradeInputMapper.mapToTradeOutput(tradeInput);

        assert tradeOutput.getProductName().equals("Treasury Bills Domestic");
        assert tradeOutput.getCurrency().equals("EUR");
        assert tradeOutput.getPrice().equals(new BigDecimal("10.0"));
    }

    @Test
    public void testGetProductNameWithMissingProductId() {
        TradeInput tradeInput = TradeInput.builder()
                                          .date("20160101")
                                          .productId("999")
                                          .currency("EUR")
                                          .price(new BigDecimal("10.0"))
                                          .build();

        String productName = tradeInputMapper.mapToTradeOutput(tradeInput).getProductName();
        assert productName.equals("Missing Product Name");
    }

    @Test
    public void testParseCsvToFlux() {
        String csvContent = "date,product_id,currency,price\n20160101,1,EUR,10.0\n20160101,2,USD,20.1\n";
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));

        Flux<TradeInput> tradeInputFlux = tradeInputMapper.parseCsvToFlux(inputStream);

        StepVerifier.create(tradeInputFlux)
                    .expectNextMatches(tradeInput -> tradeInput.getProductId().equals("1") && tradeInput.getPrice().equals(new BigDecimal("10.0")))
                    .expectNextMatches(tradeInput -> tradeInput.getProductId().equals("2") && tradeInput.getPrice().equals(new BigDecimal("20.1")))
                    .verifyComplete();
    }
}

