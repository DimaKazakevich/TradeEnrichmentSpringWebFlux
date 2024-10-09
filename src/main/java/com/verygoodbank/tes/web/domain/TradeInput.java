package com.verygoodbank.tes.web.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
@Builder
public class TradeInput {

    private String date;
    @JsonProperty("product_id")
    private String productId;
    private String currency;
    private BigDecimal price;

    public boolean isValid() {
        return isDateValid();
    }

    private boolean isDateValid() {
        SimpleDateFormat tradeDateFormat = new SimpleDateFormat("yyyyMMdd");
        tradeDateFormat.setLenient(false);
        try {
            return tradeDateFormat.parse(date) != null;
        } catch (ParseException e) {
            log.error("Invalid date format for line: {}", this);
        }
        return false;
    }

}
