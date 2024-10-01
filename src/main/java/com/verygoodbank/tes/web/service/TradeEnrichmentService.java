package com.verygoodbank.tes.web.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class TradeEnrichmentService {

    private final Map<String, String> products = new ConcurrentHashMap<>();

    public TradeEnrichmentService() {
        loadProductMapping();
    }

    public Flux<String> enrich(String tradeContentCSV) {
        BufferedReader reader = new BufferedReader(new StringReader(tradeContentCSV));
        return Flux.fromStream(reader.lines())
                .skip(1)
                .mapNotNull(this::mapToCsvRow)
                .startWith("date,product_name,currency,price\n");
    }

    /**
     * Since we have a limitation of products up to 100k then loading into memory will speed up the search
     */
    void loadProductMapping() {
        if (productsFileHasMoreThan100kRows()) {
            throw new RuntimeException("Product.csv has more than 100_000 products. Check the contents of the file " +
                    "or update the requirements otherwise there may be out of memory issue.");
        }

        try (InputStream inputStream = getClass().getResourceAsStream("/product.csv")) {
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                reader.lines().skip(1).forEach(line -> {
                    String[] fields = line.split(",");
                    String productId = fields[0].trim();
                    String productName = fields[1].trim();
                    products.put(productId, productName);
                });
            }
        } catch (Exception e) {
            throw new RuntimeException("Error loading product.csv: " + e.getMessage(), e);
        }
    }

    private boolean productsFileHasMoreThan100kRows() {
        int lines = 0;
        try (InputStream inputStream = getClass().getResourceAsStream("/product.csv")) {
            if (inputStream != null) {
                LineNumberReader lineNumberReader = new LineNumberReader(new InputStreamReader(inputStream));
                lineNumberReader.skip(Long.MAX_VALUE);
                lines = lineNumberReader.getLineNumber();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return lines > 100_000;
    }

    private String mapToCsvRow(String line) {
        String[] fields = splitCsvLine(line);
        if (fields == null) {
            log.error("Invalid values amount in row: {}", line);
            return null;
        }

        String tradeDate = fields[0].trim();
        String tradeProductId = fields[1].trim();
        String tradeCurrency = fields[2].trim();
        String tradePrice = fields[3].trim();

        Date date = validateDate(tradeDate, line);
        if (date == null) {
            return null;
        }

        String tradeProductName = getProductName(tradeProductId, line);

        return formatCsvRow(date, tradeProductName, tradeCurrency, tradePrice);
    }

    private String[] splitCsvLine(String line) {
        String[] fields = line.split(",");
        return fields.length >= 4 ? fields : null;
    }

    private Date validateDate(String tradeDate, String line) {
        SimpleDateFormat tradeDateFormat = new SimpleDateFormat("yyyyMMdd");
        tradeDateFormat.setLenient(false);
        try {
            return tradeDateFormat.parse(tradeDate);
        } catch (ParseException e) {
            log.error("Invalid date format for line: {}", line);
            return null;
        }
    }

    private String getProductName(String tradeProductId, String line) {
        String productName = products.getOrDefault(tradeProductId, "Missing Product Name");
        if ("Missing Product Name".equals(productName)) {
            log.warn("Missing product name mapping; Product ID {} not found in product.csv", tradeProductId);
        }
        return productName;
    }

    private String formatCsvRow(Date date, String tradeProductName, String tradeCurrency, String tradePrice) {
        SimpleDateFormat tradeDateFormat = new SimpleDateFormat("yyyyMMdd");
        return String.format("%s,%s,%s,%s\n", tradeDateFormat.format(date), tradeProductName, tradeCurrency, tradePrice);
    }
}

