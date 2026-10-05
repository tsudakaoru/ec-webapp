package com.example.ec.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.ec.repository.entity.ProductEntity;

/**
 * Provides database access for products.
 */
@Mapper
public interface ProductRepository {

	/**
	 * Finds all products currently offered for sale.
	 *
	 * @return products whose status is ON_SALE
	 */
	List<ProductEntity> findOnSaleProducts();

	/**
	 * Finds a product by its identifier, regardless of its sale status.
	 *
	 * @param productId product identifier
	 * @return the matching product, or {@code null} if none exists
	 */
	ProductEntity findById(@Param("productId") Long productId);

	/**
	 * Finds a product by identifier only when it is currently on sale.
	 *
	 * @param productId product identifier
	 * @return the matching sale product, or {@code null} if none exists
	 */
	ProductEntity findOnSaleById(@Param("productId") Long productId);

	/**
	 * Decreases stock only if the product remains on sale with enough stock.
	 *
	 * @param productId product identifier
	 * @param quantity quantity to reserve
	 * @return number of updated rows
	 */
	int decreaseStockIfAvailable(@Param("productId") Long productId, @Param("quantity") int quantity);
}