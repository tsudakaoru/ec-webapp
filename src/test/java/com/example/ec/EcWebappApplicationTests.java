package com.example.ec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.ec.exception.InsufficientStockException;
import com.example.ec.exception.ProductNotAvailableException;
import com.example.ec.service.CartService;
import com.example.ec.service.ProductService;
import com.example.ec.service.model.ProductModel;

@SpringBootTest
@AutoConfigureMockMvc
class EcWebappApplicationTests {

	@Autowired
	private ProductService productService;

	@Autowired
	private CartService cartService;

	@Autowired
	private MockMvc mockMvc;

	/**
	 * Verifies that the products page renders database products and external assets.
	 *
	 * @throws Exception if the MVC request fails
	 */
	@Test
	void productsPageRendersOnSaleProductsAndExternalAssets() throws Exception {
		mockMvc.perform(get("/products"))
				.andExpect(status().isOk())
				.andExpect(view().name("products"))
				.andExpect(content().string(containsString("ワイヤレスイヤホン")))
				.andExpect(content().string(containsString("ゲーミングマウス")))
				.andExpect(content().string(containsString("USB-Cハブ")))
				.andExpect(content().string(not(containsString("Webカメラ"))))
				.andExpect(content().string(containsString("/css/products.css")))
				.andExpect(content().string(containsString("/js/products.js")));
	}

	/**
	 * Verifies that the cart API rejects an addition after the session reaches stock.
	 *
	 * @throws Exception if an MVC request fails
	 */
	@Test
	void cartEndpointReturnsConflictWhenSessionQuantityWouldExceedStock() throws Exception {
		MockHttpSession session = new MockHttpSession();
		for (int quantity = 0; quantity < 5; quantity++) {
			mockMvc.perform(post("/products/cart").session(session).header("X-Requested-With", "XMLHttpRequest")
					.param("productId", "2"))
					.andExpect(status().isOk());
		}
		mockMvc.perform(get("/products").session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("data-current-quantity=\"5\"")))
				.andExpect(content().string(containsString("追加済み")));

		mockMvc.perform(post("/products/cart").session(session).header("X-Requested-With", "XMLHttpRequest")
					.param("productId", "2"))
				.andExpect(status().isConflict())
				.andExpect(content().string(containsString("在庫数を超える数量はカートに追加できません。")));
	}

	/**
	 * Verifies navigation through product detail, cart, confirmation, and completion.
	 *
	 * @throws Exception if an MVC request fails
	 */
	@Test
	void customerCanCompleteAnOrderFromProductDetail() throws Exception {
		MockHttpSession session = new MockHttpSession();
		mockMvc.perform(get("/products/1").session(session))
				.andExpect(status().isOk())
				.andExpect(view().name("products-detail"))
				.andExpect(content().string(containsString("ノイズキャンセリング対応")));

		mockMvc.perform(post("/cart/items").session(session)
					.param("productId", "1").param("quantity", "1").param("returnTo", "/products/1"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/products/1"));
		mockMvc.perform(get("/cart").session(session))
				.andExpect(status().isOk())
				.andExpect(view().name("cart"))
				.andExpect(content().string(containsString("ワイヤレスイヤホン")));
		mockMvc.perform(post("/cart/items/1/update").session(session).param("quantity", "2"))
				.andExpect(status().is3xxRedirection());
		mockMvc.perform(get("/orders/confirm").session(session))
				.andExpect(status().isOk())
				.andExpect(view().name("order-confirm"))
				.andExpect(content().string(containsString("¥11,960")));
		var orderResult = mockMvc.perform(post("/orders").session(session)
					.param("customerName", "山田 太郎")
					.param("postalCode", "100-0001")
					.param("address", "東京都千代田区")
					.param("phoneNumber", "03-1234-5678"))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		String completeUrl = orderResult.getResponse().getRedirectedUrl();
		assertEquals(true, completeUrl.matches("/orders/complete/[0-9a-f-]{36}"));
		mockMvc.perform(get(completeUrl).session(session))
				.andExpect(status().isOk())
				.andExpect(view().name("order-complete"))
				.andExpect(content().string(containsString("ご注文ありがとうございます")))
				.andExpect(content().string(containsString("ご注文番号")))
				.andExpect(content().string(containsString("山田 太郎")))
				.andExpect(content().string(containsString("¥11,960")));
		assertEquals(8, productService.getOnSaleProduct(1L).stock());
	}

	@Test
	void completionPageRequiresAnOrderIdFromTheCurrentSession() throws Exception {
		mockMvc.perform(get("/orders/complete/unknown"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/products"));
	}

	@Test
	void orderConfirmationRequiresValidDeliveryInformation() throws Exception {
		MockHttpSession session = new MockHttpSession();
		mockMvc.perform(post("/cart/items").session(session)
					.param("productId", "1").param("quantity", "1"))
				.andExpect(status().is3xxRedirection());
		mockMvc.perform(post("/orders").session(session)
					.param("customerName", "")
					.param("postalCode", "invalid")
					.param("address", "")
					.param("phoneNumber", "abc"))
				.andExpect(status().isOk())
				.andExpect(view().name("order-confirm"))
				.andExpect(content().string(containsString("氏名を入力してください。")))
				.andExpect(content().string(containsString("郵便番号は123-4567の形式で入力してください。")));
	}

	@Test
	void cartItemCanBeUpdatedAndDeletedUsingSpecifiedRoutes() throws Exception {
		MockHttpSession session = new MockHttpSession();
		mockMvc.perform(post("/cart/items").session(session)
					.param("productId", "1").param("quantity", "1"))
				.andExpect(status().is3xxRedirection());
		mockMvc.perform(post("/cart/items/1/update").session(session).param("quantity", "3"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/cart"));
		mockMvc.perform(get("/cart").session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("value=\"3\"")));
		mockMvc.perform(post("/cart/items/1/delete").session(session))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/cart"));
		mockMvc.perform(get("/cart").session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("カートは空です")));
	}

	@Test
	void cartAdditionAcceptsQuantityAndRejectsExternalReturnPath() throws Exception {
		MockHttpSession session = new MockHttpSession();
		mockMvc.perform(post("/cart/items").session(session)
					.param("productId", "1").param("quantity", "2").param("returnTo", "https://example.com"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/cart"));
		mockMvc.perform(get("/cart").session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("name=\"quantity\" min=\"1\" max=\"10\" value=\"2\"")))
				.andExpect(content().string(containsString("¥11,960")));
	}

	/**
	 * Verifies that the product catalog contains only products on sale.
	 */
	@Test
	void getOnSaleProductsReturnsOnlyProductsOnSale() {
		List<ProductModel> products = productService.getOnSaleProducts();

		assertEquals(List.of(1L, 2L, 3L), products.stream().map(ProductModel::productId).toList());
		assertEquals(List.of("ON_SALE", "ON_SALE", "ON_SALE"),
				products.stream().map(ProductModel::status).toList());
	}

	/**
	 * Verifies that adding up to the stock limit returns the resulting cart quantity.
	 */
	@Test
	void validateAdditionAllowsQuantityEqualToStock() {
		assertEquals(5, cartService.validateAddition(2L, 4, 1));
	}

	/**
	 * Verifies that adding a quantity beyond stock is rejected.
	 */
	@Test
	void validateAdditionRejectsQuantityBeyondStock() {
		assertThrows(InsufficientStockException.class,
				() -> cartService.validateAddition(2L, 4, 2));
	}

	/**
	 * Verifies that an out-of-stock product cannot be added to the cart.
	 */
	@Test
	void validateAdditionRejectsOutOfStockProduct() {
		assertThrows(InsufficientStockException.class,
				() -> cartService.validateAddition(3L, 0, 1));
	}

	/**
	 * Verifies that a stopped product cannot be added to the cart.
	 */
	@Test
	void validateAdditionRejectsProductNotOnSale() {
		assertThrows(ProductNotAvailableException.class,
				() -> cartService.validateAddition(4L, 0, 1));
	}

}
