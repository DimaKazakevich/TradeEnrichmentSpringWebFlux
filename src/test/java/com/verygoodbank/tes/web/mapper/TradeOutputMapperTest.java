package com.verygoodbank.tes.web.mapper;

import com.verygoodbank.tes.web.domain.TradeOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class TradeOutputMapperTest {

    private TradeOutputMapper tradeOutputMapper;

    @BeforeEach
    public void setUp() {
        tradeOutputMapper = new TradeOutputMapper();
    }

    @Test
    public void testMapToCsv() {
        TradeOutput tradeOutput = TradeOutput.builder()
                                             .date("20160101")
                                             .productName("Treasury Bills Domestic")
                                             .currency("EUR")
                                             .price(new BigDecimal("10.0"))
                                             .build();

        String csvResult = tradeOutputMapper.mapToCsv(tradeOutput);

        assertEquals("20160101,Treasury Bills Domestic,EUR,10.0\n", csvResult);
    }

    @Test
    public void testGetCsvHeader() {
        String header = tradeOutputMapper.getCsvHeader();
        assertEquals("date,product_name,currency,price\n", header);
    }
}

