package com.ddd.programmersjk.domain.product;

import com.ddd.programmersjk.domain.category.CategoryRepository;
import com.ddd.programmersjk.domain.member.Member;
import com.ddd.programmersjk.domain.order.Order;
import com.ddd.programmersjk.domain.order.OrderRepository;
import com.ddd.programmersjk.domain.product.dto.ProductRequest;
import com.ddd.programmersjk.domain.store.Store;
import com.ddd.programmersjk.domain.store.StoreRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;

    public ProductService(ProductRepository productRepository, StoreRepository storeRepository) {
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
    }

    public Page<Product> getProductsOfCategory(Long categoryId, int page, int size) {
        return productRepository.findByCategoryId(categoryId, PageRequest.of(page, size));
    }

    @Transactional
    public void register(ProductRequest request) {
        Store store = findStoreById(request.getStoreId());
        if (store.isFired()) {
            throw new IllegalArgumentException("불나서 상점 망함");
        }
        productRepository.save(new Product());
    }

    private Store findStoreById(Long id) {
        return storeRepository.findById(id)
                .orElseThrow(IllegalArgumentException::new);
    }
}
