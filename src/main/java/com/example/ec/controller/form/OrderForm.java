package com.example.ec.controller.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OrderForm(
		@NotBlank(message = "氏名を入力してください。")
		@Size(max = 100, message = "氏名は100文字以内で入力してください。")
		String customerName,
		@NotBlank(message = "郵便番号を入力してください。")
		@Pattern(regexp = "[0-9]{3}-?[0-9]{4}", message = "郵便番号は123-4567の形式で入力してください。")
		String postalCode,
		@NotBlank(message = "住所を入力してください。")
		@Size(max = 255, message = "住所は255文字以内で入力してください。")
		String address,
		@NotBlank(message = "電話番号を入力してください。")
		@Pattern(regexp = "[0-9-]{10,13}", message = "電話番号を正しく入力してください。")
		String phoneNumber) {

	public static OrderForm empty() {
		return new OrderForm("", "", "", "");
	}
}