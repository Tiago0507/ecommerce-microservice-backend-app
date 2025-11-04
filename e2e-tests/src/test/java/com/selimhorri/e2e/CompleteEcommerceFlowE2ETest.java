package com.selimhorri.e2e;

import com.selimhorri.e2e.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E Test 5: Complete E-commerce Flow - All Services Integration")
public class CompleteEcommerceFlowE2ETest extends AbstractE2ETest {

    private static Integer userId;
    private static Integer productId;
    private static Integer cartId;
    private static Integer orderId;
    private static Integer paymentId;

    @Test
    @Order(1)
    @DisplayName("Step 1: User Registration")
    void step1_UserRegistration() {
        UserDto userDto = UserDto.builder()
                .firstName("Charlie")
                .lastName("Brown")
                .email("charlie.b@test.com")
                .phone("5552223333")
                .build();

        ResponseEntity<UserDto> response = restTemplate.postForEntity(
                getUserServiceUrl(), userDto, UserDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        userId = response.getBody().getUserId();
        
        System.out.println("✅ Step 1: User registered with ID " + userId);
    }

    @Test
    @Order(2)
    @DisplayName("Step 2: Browse Product Catalog")
    void step2_BrowseProducts() {
        ProductDto productDto = ProductDto.builder()
                .productTitle("Mechanical Keyboard")
                .imageUrl("http://test.com/keyboard.jpg")
                .sku("KEY-001")
                .priceUnit(149.99)
                .quantity(75)
                .build();

        ResponseEntity<ProductDto> response = restTemplate.postForEntity(
                getProductServiceUrl(), productDto, ProductDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        productId = response.getBody().getProductId();
        
        System.out.println("✅ Step 2: Product browsed with ID " + productId);
    }

    @Test
    @Order(3)
    @DisplayName("Step 3: Add Product to Favourites")
    void step3_AddToFavourites() {
        FavouriteDto favouriteDto = FavouriteDto.builder()
                .userId(userId)
                .productId(productId)
                .likeDate(LocalDateTime.now())
                .userDto(UserDto.builder().userId(userId).build())
                .productDto(ProductDto.builder().productId(productId).build())
                .build();

        ResponseEntity<FavouriteDto> response = restTemplate.postForEntity(
                getFavouriteServiceUrl(), favouriteDto, FavouriteDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        System.out.println("✅ Step 3: Product added to favourites");
    }

    @Test
    @Order(4)
    @DisplayName("Step 4: Create Shopping Cart")
    void step4_CreateCart() {
        CartDto cartDto = CartDto.builder().userId(userId).build();

        ResponseEntity<CartDto> response = restTemplate.postForEntity(
                getCartServiceUrl(), cartDto, CartDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        cartId = response.getBody().getCartId();
        
        System.out.println("✅ Step 4: Cart created with ID " + cartId);
    }

    @Test
    @Order(5)
    @DisplayName("Step 5: Place Order")
    void step5_PlaceOrder() {
        OrderDto orderDto = OrderDto.builder()
                .orderDesc("Mechanical Keyboard Order")
                .orderFee(149.99)
                .orderStatus("ORDERED")
                .cartDto(CartDto.builder().cartId(cartId).build())
                .build();

        ResponseEntity<OrderDto> response = restTemplate.postForEntity(
                getOrderServiceUrl(), orderDto, OrderDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        orderId = response.getBody().getOrderId();
        
        System.out.println("✅ Step 5: Order placed with ID " + orderId);
    }

    @Test
    @Order(6)
    @DisplayName("Step 6: Process Payment - Validates Order Service Communication")
    void step6_ProcessPayment() {
        PaymentDto paymentDto = PaymentDto.builder()
                .isPayed(true)
                .paymentStatus("COMPLETED")
                .orderDto(OrderDto.builder().orderId(orderId).build())
                .build();

        ResponseEntity<PaymentDto> response = restTemplate.postForEntity(
                getPaymentServiceUrl(), paymentDto, PaymentDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        paymentId = response.getBody().getPaymentId();
        assertThat(response.getBody().getIsPayed()).isTrue();
        
        System.out.println("✅ Step 6: Payment processed with ID " + paymentId);
    }

    @Test
    @Order(7)
    @DisplayName("Step 7: Verify Order Status After Payment")
    void step7_VerifyOrderStatus() {
        ResponseEntity<OrderDto> response = restTemplate.getForEntity(
                getOrderServiceUrl() + "/" + orderId, OrderDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getOrderStatus()).isIn("DELIVERED", "ORDERED");
        
        System.out.println("✅ Step 7: Order status verified: " + response.getBody().getOrderStatus());
    }

    @Test
    @Order(8)
    @DisplayName("Step 8: Create Shipping for Order")
    void step8_CreateShipping() {
        OrderItemDto orderItemDto = OrderItemDto.builder()
                .productId(productId)
                .orderId(orderId)
                .orderedQuantity(1)
                .productDto(ProductDto.builder().productId(productId).build())
                .orderDto(OrderDto.builder().orderId(orderId).build())
                .build();

        ResponseEntity<OrderItemDto> response = restTemplate.postForEntity(
                getShippingServiceUrl(), orderItemDto, OrderItemDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        System.out.println("✅ Step 8: Shipping created for order");
    }

    @Test
    @Order(9)
    @DisplayName("Step 9: Verify Complete Data Integration - Shipping with Product & Order Details")
    void step9_VerifyShippingWithCompleteData() {
        ResponseEntity<OrderItemDto> response = restTemplate.getForEntity(
                getShippingServiceUrl() + "/" + orderId + "/" + productId,
                OrderItemDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getProductDto()).isNotNull();
        assertThat(response.getBody().getOrderDto()).isNotNull();
        
        System.out.println("✅ Step 9: Shipping retrieved with complete product and order data");
    }

    @Test
    @Order(10)
    @DisplayName("Step 10: Verify Payment Record with Complete Order Details")
    void step10_VerifyPaymentWithOrderDetails() {
        ResponseEntity<PaymentDto> response = restTemplate.getForEntity(
                getPaymentServiceUrl() + "/" + paymentId, PaymentDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getOrderDto()).isNotNull();
        
        System.out.println("✅ Step 10: Payment retrieved with complete order data");
        System.out.println("🎉 COMPLETE E-COMMERCE FLOW SUCCESSFUL - ALL 6 SERVICES INTEGRATED!");
    }
}