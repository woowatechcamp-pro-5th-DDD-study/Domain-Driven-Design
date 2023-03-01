package com.ddd.programmersjk.domain.product.dto;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

public class ProductRequest {
    private Long storeId;

    public Long getStoreId() {
        return storeId;
    }
}
