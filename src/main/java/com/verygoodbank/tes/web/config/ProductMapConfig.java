package com.verygoodbank.tes.web.config;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.verygoodbank.tes.web.domain.Product;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class ProductMapConfig {

    @Value("${product-mapping-file.max-capacity}")
    private Integer PRODUCT_MAPPING_FILE_MAX_CAPACITY;

    /**
     * Since we have a limitation of products up to 100k then loading into memory will speed up the search
     */
    @Bean
    public ConcurrentHashMap<String, String> loadProductMapping(@Value("classpath:/product.csv") Resource productsCsv) {
        if (productsFileHasMoreThanMaxCapacityRows(productsCsv)) {
            throw new RuntimeException(
                    String.format("%s has more than %d products. Check the contents of the file " +
                                    "or update the requirements otherwise there may be out of memory issue.",
                            productsCsv.getFilename(), PRODUCT_MAPPING_FILE_MAX_CAPACITY));
        }

        ConcurrentHashMap<String, String> products = new ConcurrentHashMap<>();

        try (InputStream inputStream = productsCsv.getInputStream()) {
            CsvMapper csvMapper = new CsvMapper();
            CsvSchema schema = CsvSchema.emptySchema().withHeader();
            MappingIterator<Product> iterator = csvMapper.readerFor(Product.class)
                                                         .with(schema)
                                                         .readValues(inputStream);
            iterator.forEachRemaining(
                    product -> products.put(product.getProductId(), product.getProductName()));
        } catch (Exception e) {
            throw new RuntimeException("Error loading product.csv: " + e.getMessage(), e);
        }

        return products;
    }

    private boolean productsFileHasMoreThanMaxCapacityRows(@Value("classpath:/product.csv") Resource productsCsv) {
        int lines;
        try (InputStream inputStream = productsCsv.getInputStream()) {
            LineNumberReader lineNumberReader = new LineNumberReader(new InputStreamReader(inputStream));
            lineNumberReader.skip(Long.MAX_VALUE);
            lines = lineNumberReader.getLineNumber();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return lines > PRODUCT_MAPPING_FILE_MAX_CAPACITY;
    }

}
