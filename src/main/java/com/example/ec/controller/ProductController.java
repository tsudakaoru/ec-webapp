package com.example.ec.controller;

import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.ec.service.model.CartItemModel;
import com.example.ec.service.CartService;
import com.example.ec.service.ProductService;
import com.example.ec.service.model.ProductModel;

/**
 * Handles product catalog and cart addition requests.
 */
@Controller
public class ProductController {

	private static final String CART_SESSION_KEY = "cartQuantities";
	private static final String ORDER_SESSION_KEY = "completedOrderItems";

	@Autowired
	private ProductService productService;

	@Autowired
	private CartService cartService;

	/**
	 * Displays the products currently available for sale.
	 *
	 * @param session current user session
	 * @param model view model
	 * @return product list template name
	 */
	@GetMapping("/products")
	public String showProducts(HttpSession session, Model model) {
		List<ProductModel> products = productService.getOnSaleProducts();
		Map<Long, Integer> sessionCart = getCartQuantities(session);
		Map<Long, Integer> cartQuantities = new LinkedHashMap<>();
		for (ProductModel product : products) {
			cartQuantities.put(product.productId(), sessionCart.getOrDefault(product.productId(), 0));
		}
		int cartCount = sessionCart.values().stream().mapToInt(Integer::intValue).sum();

		model.addAttribute("products", products);
		model.addAttribute("cartQuantities", cartQuantities);
		model.addAttribute("cartCount", cartCount);
		return "products";
	}

	/**
	 * Displays details for one product currently on sale.
	 *
	 * @param productId product identifier
	 * @param session current user session
	 * @param model view model
	 * @return product detail template name
	 */
	@GetMapping("/products/{productId}")
	public String showProductDetail(@PathVariable Long productId, HttpSession session, Model model) {
		ProductModel product = productService.getOnSaleProduct(productId);
		Map<Long, Integer> cartQuantities = getCartQuantities(session);
		model.addAttribute("product", product);
		model.addAttribute("currentQuantity", cartQuantities.getOrDefault(productId, 0));
		model.addAttribute("cartCount", cartQuantities.values().stream().mapToInt(Integer::intValue).sum());
		return "products-detail";
	}

	/**
	 * Displays the current session cart.
	 *
	 * @param session current user session
	 * @param model view model
	 * @return cart template name
	 */
	@GetMapping("/cart")
	public String showCart(HttpSession session, Model model) {
		Map<Long, Integer> cartQuantities = getCartQuantities(session);
		List<CartItemModel> cartItems = cartService.getCartItems(cartQuantities);
		model.addAttribute("cartItems", cartItems);
		model.addAttribute("cartCount", getCartCount(cartQuantities));
		model.addAttribute("cartTotal", getCartTotal(cartItems));
		return "cart";
	}

	/**
	 * Updates a line quantity in the session cart.
	 *
	 * @param productId product identifier
	 * @param quantity requested quantity
	 * @param session current user session
	 * @return redirect to the cart
	 */
	@PostMapping("/cart/update")
	public String updateCartQuantity(@RequestParam Long productId, @RequestParam int quantity, HttpSession session) {
		cartService.validateQuantity(productId, quantity);
		getCartQuantities(session).put(productId, quantity);
		return "redirect:/cart";
	}

	/**
	 * Removes a product from the session cart.
	 *
	 * @param productId product identifier
	 * @param session current user session
	 * @return redirect to the cart
	 */
	@PostMapping("/cart/remove")
	public String removeCartItem(@RequestParam Long productId, HttpSession session) {
		getCartQuantities(session).remove(productId);
		return "redirect:/cart";
	}

	@PostMapping("/cart/items")
	public String addCartItem(@RequestParam Long productId, @RequestParam int quantity,
			@RequestParam(defaultValue = "/cart") String returnTo, HttpSession session) {
		Map<Long, Integer> cartQuantities = getCartQuantities(session);
		int updatedQuantity = cartService.validateAddition(productId,
				cartQuantities.getOrDefault(productId, 0), quantity);
		cartQuantities.put(productId, updatedQuantity);
		return "redirect:" + safeReturnPath(returnTo);
	}

	/**
	 * Displays order confirmation after rechecking every cart line against stock.
	 *
	 * @param session current user session
	 * @param model view model
	 * @return order confirmation template or redirect to an empty cart
	 */
	@GetMapping("/orders/confirm")
	public String showOrderConfirmation(HttpSession session, Model model) {
		Map<Long, Integer> cartQuantities = getCartQuantities(session);
		if (cartQuantities.isEmpty()) {
			return "redirect:/cart";
		}
		List<CartItemModel> cartItems = cartService.getCartItems(cartQuantities);
		model.addAttribute("cartItems", cartItems);
		model.addAttribute("cartCount", getCartCount(cartQuantities));
		model.addAttribute("cartTotal", getCartTotal(cartItems));
		return "order-confirmation";
	}

	/**
	 * Reserves stock, clears the cart, and saves the order summary in the session.
	 *
	 * @param session current user session
	 * @return redirect to the order completion page
	 */
	@PostMapping("/orders")
	public String placeOrder(HttpSession session) {
		Map<Long, Integer> cartQuantities = getCartQuantities(session);
		List<CartItemModel> completedOrder = cartService.completeOrder(cartQuantities);
		session.setAttribute(ORDER_SESSION_KEY, new ArrayList<>(completedOrder));
		cartQuantities.clear();
		return "redirect:/orders/complete";
	}

	/**
	 * Displays the most recently completed order from this session.
	 *
	 * @param session current user session
	 * @param model view model
	 * @return order completion template or redirect to the product list
	 */
	@SuppressWarnings("unchecked")
	@GetMapping("/orders/complete")
	public String showOrderComplete(HttpSession session, Model model) {
		Object savedOrder = session.getAttribute(ORDER_SESSION_KEY);
		if (!(savedOrder instanceof List<?>)) {
			return "redirect:/products";
		}
		List<CartItemModel> orderItems = (List<CartItemModel>) savedOrder;
		model.addAttribute("orderItems", orderItems);
		model.addAttribute("orderTotal", getCartTotal(orderItems));
		model.addAttribute("cartCount", getCartCount(getCartQuantities(session)));
		return "order-complete";
	}

	/**
	 * Adds one unit of a product to the session cart after checking stock.
	 *
	 * @param productId product identifier
	 * @param session current user session
	 * @return the updated total number of units in the cart
	 */
	@PostMapping(value = "/products/cart", produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> addToCart(@RequestParam Long productId, HttpSession session) {
		Map<Long, Integer> cartQuantities = getCartQuantities(session);
		int currentQuantity = cartQuantities.getOrDefault(productId, 0);
		int updatedQuantity = cartService.validateAddition(productId, currentQuantity, 1);
		cartQuantities.put(productId, updatedQuantity);
		int cartCount = cartQuantities.values().stream().mapToInt(Integer::intValue).sum();
		return ResponseEntity.ok()
				.contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
				.body(String.valueOf(cartCount));
	}

	/**
	 * Gets or creates the current session's product quantity map.
	 *
	 * @param session current user session
	 * @return mutable product quantities for the session cart
	 */
	@SuppressWarnings("unchecked")
	private Map<Long, Integer> getCartQuantities(HttpSession session) {
		Object existingCart = session.getAttribute(CART_SESSION_KEY);
		if (existingCart instanceof Map<?, ?>) {
			return (Map<Long, Integer>) existingCart;
		}

		Map<Long, Integer> cartQuantities = new LinkedHashMap<>();
		session.setAttribute(CART_SESSION_KEY, cartQuantities);
		return cartQuantities;
	}

	/**
	 * Calculates the number of product units in a cart.
	 *
	 * @param quantities product quantities
	 * @return total units
	 */
	private int getCartCount(Map<Long, Integer> quantities) {
		return quantities.values().stream().mapToInt(Integer::intValue).sum();
	}

	/**
	 * Calculates the total amount for cart or order items.
	 *
	 * @param items cart or order items
	 * @return total amount
	 */
	private BigDecimal getCartTotal(List<CartItemModel> items) {
		return items.stream().map(CartItemModel::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private String safeReturnPath(String returnTo) {
		if (returnTo != null && returnTo.matches("/products(?:/\\d+)?|/cart")) {
			return returnTo;
		}
		return "/cart";
	}
}