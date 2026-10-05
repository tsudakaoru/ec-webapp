package com.example.ec.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.ec.repository.ProductRepository;
import com.example.ec.repository.entity.ProductEntity;
import com.example.ec.service.model.ProductModel;

/**
 * Provides product catalog business logic.
 */
@Service
public class ProductService {

	@Autowired
	private ProductRepository productRepository;

	/**
	 * Returns products available for sale as service-layer models.
	 *
	 * @return products whose status is ON_SALE
	 */
	public List<ProductModel> getOnSaleProducts() {
		return productRepository.findOnSaleProducts().stream()
				.map(this::toModel)
				.toList();
	}

	/**
	 * Returns a product only when it is currently available for sale.
	 *
	 * @param productId product identifier
	 * @return product service model
	 * @throws com.example.ec.exception.ProductNotAvailableException if the product is missing or stopped
	 */
	public ProductModel getOnSaleProduct(Long productId) {
		ProductEntity entity = productRepository.findOnSaleById(productId);
		if (entity == null) {
			throw new com.example.ec.exception.ProductNotAvailableException();
		}
		return toModel(entity);
	}

	/**
	 * Decreases stock if the product is still on sale and sufficiently stocked.
	 *
	 * @param productId product identifier
	 * @param quantity quantity to reserve
	 * @return true if stock was decreased
	 */
	public boolean decreaseStockIfAvailable(Long productId, int quantity) {
		return productRepository.decreaseStockIfAvailable(productId, quantity) == 1;
	}

	/**
	 * Converts a persistence entity to a service model.
	 *
	 * @param entity product persistence entity
	 * @return product service model
	 */
	private ProductModel toModel(ProductEntity entity) {
		return new ProductModel(
				entity.getProductId(),
				entity.getName(),
				entity.getDescription(),
				entity.getPrice(),
				entity.getStock(),
				entity.getStatus(),
				entity.getImageUrl());
	}
}