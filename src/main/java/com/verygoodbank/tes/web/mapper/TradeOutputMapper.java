package com.verygoodbank.tes.web.mapper;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.verygoodbank.tes.web.domain.TradeOutput;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;

@Component
public class TradeOutputMapper {

    public String mapToCsv(TradeOutput tradeOutput) {
        CsvMapper csvMapper = new CsvMapper();
        StringWriter writer = new StringWriter();
        CsvSchema schema = csvMapper.schemaFor(TradeOutput.class).withoutQuoteChar();
        try {
            csvMapper.writer(schema).writeValue(writer, tradeOutput);
        } catch (IOException e) {
            throw new RuntimeException("Error writing TradeOutput using StringWriter", e);
        }

        return writer.toString();
    }

    public String getCsvHeader() {
        CsvMapper csvMapper = new CsvMapper();
        StringWriter writer = new StringWriter();
        CsvSchema schema = csvMapper.schemaFor(TradeOutput.class).withHeader();
        try {
            csvMapper.writer(schema).writeValue(writer, null);
        } catch (IOException e) {
            throw new RuntimeException("Error writing csv header using StringWriter", e);
        }

        return writer.toString();
    }

}

