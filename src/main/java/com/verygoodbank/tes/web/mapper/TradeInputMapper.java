package com.verygoodbank.tes.web.mapper;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.verygoodbank.tes.web.domain.TradeInput;
import com.verygoodbank.tes.web.domain.TradeOutput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TradeInputMapper {

    private final Map<String, String> products;

    public Flux<TradeInput> mapToTradeInputFlux(Flux<DataBuffer> csvDataBufferFlux) {
        InputStream inputStream = getInputStreamFromFluxDataBuffer(csvDataBufferFlux);

        return parseCsvToFlux(inputStream)
                .publishOn(Schedulers.boundedElastic())
                .doFinally(signalType -> {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        throw new RuntimeException("Error closing InputStream", e);
                    }
                })
                .doOnError(error -> log.error("error mapToTradeInputFlux()", error))
                .onErrorResume(e -> Flux.error(new Exception(e.getMessage())));
    }

    InputStream getInputStreamFromFluxDataBuffer(Flux<DataBuffer> data) {
        PipedOutputStream pipedOutputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream;
        try {
            pipedInputStream = new PipedInputStream(pipedOutputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        DataBufferUtils.write(data, pipedOutputStream)
                       .publishOn(Schedulers.boundedElastic())
                       .doFinally((s) -> {
                           try {
                               pipedOutputStream.close();
                           } catch (IOException e) {
                               throw new RuntimeException("Error closing PipedOutputStream", e);
                           }
                       })
                       .doOnError(error -> log.error("error getInputStreamFromFluxDataBuffer()", error))
                       .onErrorResume(e -> Flux.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                               e.getMessage())))
                       .subscribe(DataBufferUtils.releaseConsumer());


        return pipedInputStream;
    }

    Flux<TradeInput> parseCsvToFlux(InputStream inputStream) {
        CsvMapper csvMapper = new CsvMapper();
        CsvSchema schema = CsvSchema.emptySchema().withHeader();

        MappingIterator<TradeInput> iterator;
        try {
            iterator = csvMapper.readerFor(TradeInput.class)
                                .with(schema)
                                .readValues(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Error reading InputStream", e);
        }

        return Flux.<TradeInput>generate(sink -> {
                       if (iterator.hasNext()) {
                           sink.next(iterator.next());
                       } else {
                           sink.complete();
                       }
                   })
                   .subscribeOn(Schedulers.boundedElastic())
                   .doOnError(error -> log.error("error parseCsvToFlux()", error))
                   .onErrorResume(e -> Flux.error(new Exception(e.getMessage())));
    }

    public TradeOutput mapToTradeOutput(TradeInput tradeInput) {
        return TradeOutput
                .builder()
                .date(tradeInput.getDate())
                .productName(getProductName(tradeInput))
                .currency(tradeInput.getCurrency())
                .price(tradeInput.getPrice())
                .build();
    }

    private String getProductName(TradeInput tradeInput) {
        String productName = products.getOrDefault(tradeInput.getProductId(), "Missing Product Name");
        if ("Missing Product Name".equals(productName)) {
            log.warn("Missing product name mapping; Product ID {} not found in product.csv", tradeInput.getProductId());
        }
        return productName;
    }

}

