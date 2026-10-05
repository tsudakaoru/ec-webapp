package com.example.ec.service.model;

import java.math.BigDecimal;

/**
 * Product and quantity information displayed in cart and order views.
 */
public record CartItemModel(ProductModel product, int quantity, BigDecimal subtotal) {
}