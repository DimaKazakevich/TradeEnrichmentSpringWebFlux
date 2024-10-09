package com.verygoodbank.tes.web.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TradeOutput {

    @JsonProperty(index = 1)
    private String date;
    @JsonProperty(value = "product_name", index = 2)
    private String productName;
    @JsonProperty(index = 3)
    private String currency;
    @JsonProperty(index = 4)
    private BigDecimal price;

}
