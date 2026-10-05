package com.example.ec.repository.entity;

import java.math.BigDecimal;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Products table row mapped by MyBatis.
 */
@Data
@NoArgsConstructor
public class ProductEntity {
	private Long productId;
	private String name;
	private String description;
	private BigDecimal price;
	private Integer stock;
	private String status;
	private String imageUrl;
}