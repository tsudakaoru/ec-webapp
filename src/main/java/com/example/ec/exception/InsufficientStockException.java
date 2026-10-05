package com.example.ec.exception;

/**
 * Indicates that a requested cart quantity exceeds the available stock.
 */
public class InsufficientStockException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	/**
	 * Creates the exception with its message key for the global exception handler.
	 */
	public InsufficientStockException() {
		super("cart.stock.exceeded");
	}
}