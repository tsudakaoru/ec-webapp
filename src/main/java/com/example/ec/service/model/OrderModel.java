package com.example.ec.service.model;

import java.math.BigDecimal;
import java.util.List;

public record OrderModel(String orderId, String customerName, String postalCode, String address,
		String phoneNumber, List<CartItemModel> items, BigDecimal total) {

	public OrderModel {
		items = List.copyOf(items);
	}
}