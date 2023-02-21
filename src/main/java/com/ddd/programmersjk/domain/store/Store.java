package com.ddd.programmersjk.domain.store;

import com.ddd.programmersjk.domain.product.Product;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Store {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public boolean isFired() {
        return true;
    }

    private Product createProduct() {
        if (isFired()) {
            throw new IllegalArgumentException();
        }

        return new Product();
    }
}
