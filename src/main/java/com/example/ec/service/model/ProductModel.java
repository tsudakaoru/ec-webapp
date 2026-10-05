package com.example.ec.service.model;

import java.math.BigDecimal;

/**
 * Product data exposed by the service layer to controllers.
 */
public record ProductModel(
		Long productId,
		String name,
		String description,
		BigDecimal price,
		Integer stock,
		String status,
		String imageUrl) {
}