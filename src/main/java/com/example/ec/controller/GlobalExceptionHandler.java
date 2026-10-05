package com.example.ec.controller;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.ui.Model;

import com.example.ec.exception.InsufficientStockException;
import com.example.ec.exception.ProductNotAvailableException;

/**
 * Handles business exceptions for MVC controllers.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

	@Autowired
	private MessageSource messageSource;

	/**
	 * Resolves a business exception message for the cart API response.
	 *
	 * @param exception handled business exception
	 * @param locale request locale
	 * @return conflict response with a localized error message
	 */
	@ExceptionHandler({InsufficientStockException.class, ProductNotAvailableException.class})
	public Object handleProductBusinessException(RuntimeException exception, Locale locale,
			HttpServletRequest request, Model model) {
		String message = messageSource.getMessage(exception.getMessage(), null, locale);
		if (!"XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
			model.addAttribute("errorMessage", message);
			return "error";
		}
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
				.body(message);
	}
}