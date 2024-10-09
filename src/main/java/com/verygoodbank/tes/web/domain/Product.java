package com.verygoodbank.tes.web.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Product {

    @JsonProperty("product_id")
    private String productId;
    @JsonProperty("product_name")
    private String productName;

}
