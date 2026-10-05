package com.example.ec.exception;

/**
 * Indicates that a product cannot be added because it is not available for sale.
 */
public class ProductNotAvailableException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	/**
	 * Creates the exception with its message key for the global exception handler.
	 */
	public ProductNotAvailableException() {
		super("cart.product.not.available");
	}
}