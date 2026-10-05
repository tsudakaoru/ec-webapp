package com.example.ec.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ec.exception.InsufficientStockException;
import com.example.ec.exception.ProductNotAvailableException;
import com.example.ec.service.model.CartItemModel;
import com.example.ec.service.model.ProductModel;

/**
 * Provides cart-related business rules.
 */
@Service
public class CartService {

	@Autowired
	private ProductService productService;

	/**
	 * Validates a cart addition and returns the resulting quantity.
	 *
	 * @param productId product identifier
	 * @param currentQuantity quantity already in the cart
	 * @param quantityToAdd quantity requested for addition
	 * @return resulting cart quantity
	 * @throws IllegalArgumentException if either quantity is invalid
	 * @throws ProductNotAvailableException if the product does not exist or is not on sale
	 * @throws InsufficientStockException if the resulting quantity exceeds stock
	 */
	public int validateAddition(Long productId, int currentQuantity, int quantityToAdd) {
		if (currentQuantity < 0 || quantityToAdd <= 0) {
			throw new IllegalArgumentException("Cart quantities must be non-negative and the addition must be positive.");
		}

		ProductModel product = productService.getOnSaleProduct(productId);

		long resultingQuantity = (long) currentQuantity + quantityToAdd;
		if (resultingQuantity > product.stock()) {
			throw new InsufficientStockException();
		}
		return (int) resultingQuantity;
	}

	/**
	 * Builds view models for the products in a cart.
	 *
	 * @param quantities product quantities stored in the session
	 * @return cart items in session order
	 */
	public List<CartItemModel> getCartItems(Map<Long, Integer> quantities) {
		List<CartItemModel> items = new ArrayList<>();
		for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
			ProductModel product = productService.getOnSaleProduct(entry.getKey());
			items.add(toCartItem(product, entry.getValue()));
		}
		return items;
	}

	/**
	 * Updates a product quantity after checking sale status and stock.
	 *
	 * @param productId product identifier
	 * @param quantity requested cart quantity
	 */
	public void validateQuantity(Long productId, int quantity) {
		validateAddition(productId, 0, quantity);
	}

	/**
	 * Validates all cart lines and atomically reserves stock for an order.
	 *
	 * @param quantities product quantities stored in the session
	 * @return items captured for the order completion view
	 * @throws IllegalStateException if the cart is empty
	 */
	@Transactional
	public List<CartItemModel> completeOrder(Map<Long, Integer> quantities) {
		if (quantities.isEmpty()) {
			throw new ProductNotAvailableException();
		}

		List<CartItemModel> items = getCartItems(quantities);
		for (CartItemModel item : items) {
			if (!productService.decreaseStockIfAvailable(item.product().productId(), item.quantity())) {
				throw new InsufficientStockException();
			}
		}
		return items;
	}

	/**
	 * Creates a cart item and calculates its line subtotal.
	 *
	 * @param product product details
	 * @param quantity item quantity
	 * @return cart item view model
	 */
	private CartItemModel toCartItem(ProductModel product, int quantity) {
		return new CartItemModel(product, quantity, product.price().multiply(BigDecimal.valueOf(quantity)));
	}
}