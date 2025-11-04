package com.selimhorri.e2e;

import com.selimhorri.e2e.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E Test 3: Shipping Flow - Product → Order → Shipping")
public class ShippingFlowE2ETest extends AbstractE2ETest {

    private static Integer userId;
    private static Integer productId;
    private static Integer cartId;
    private static Integer orderId;

    @Test
    @Order(1)
    @DisplayName("Create Product in Product Service")
    void step1_CreateProduct() {
        ProductDto productDto = ProductDto.builder()
                .productTitle("Gaming Laptop")
                .imageUrl("http://test.com/laptop.jpg")
                .sku("LAP-001")
                .priceUnit(1299.99)
                .quantity(50)
                .build();

        ResponseEntity<ProductDto> response = restTemplate.postForEntity(
                getProductServiceUrl(), productDto, ProductDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        productId = response.getBody().getProductId();
        
        System.out.println("✅ Product created: ID = " + productId);
    }

    @Test
    @Order(2)
    @DisplayName("Create User, Cart and Order")
    void step2_SetupUserAndOrder() {
        // User
        UserDto userDto = UserDto.builder()
                .firstName("Alice")
                .lastName("Johnson")
                .email("alice.j@test.com")
                .phone("5551234567")
                .build();
        ResponseEntity<UserDto> userResponse = restTemplate.postForEntity(
                getUserServiceUrl(), userDto, UserDto.class);
        userId = userResponse.getBody().getUserId();

        // Cart
        CartDto cartDto = CartDto.builder().userId(userId).build();
        ResponseEntity<CartDto> cartResponse = restTemplate.postForEntity(
                getCartServiceUrl(), cartDto, CartDto.class);
        cartId = cartResponse.getBody().getCartId();

        // Order
        OrderDto orderDto = OrderDto.builder()
                .orderDesc("Laptop order")
                .orderFee(1299.99)
                .orderStatus("ORDERED")
                .cartDto(CartDto.builder().cartId(cartId).build())
                .build();
        ResponseEntity<OrderDto> orderResponse = restTemplate.postForEntity(
                getOrderServiceUrl(), orderDto, OrderDto.class);
        orderId = orderResponse.getBody().getOrderId();

        System.out.println("✅ Setup complete: User, Cart, Order created");
    }

    @Test
    @Order(3)
    @DisplayName("Create Shipping (OrderItem) - Validates Product and Order Services")
    void step3_CreateShipping() {
        OrderItemDto orderItemDto = OrderItemDto.builder()
                .productId(productId)
                .orderId(orderId)
                .orderedQuantity(2)
                .productDto(ProductDto.builder().productId(productId).build())
                .orderDto(OrderDto.builder().orderId(orderId).build())
                .build();

        ResponseEntity<OrderItemDto> response = restTemplate.postForEntity(
                getShippingServiceUrl(), orderItemDto, OrderItemDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getOrderedQuantity()).isEqualTo(2);
        
        System.out.println("✅ Shipping created for 2 units");
    }

    @Test
    @Order(4)
    @DisplayName("Retrieve Shipping with Enriched Product and Order Data")
    void step4_GetShippingWithEnrichedData() {
        ResponseEntity<OrderItemDto> response = restTemplate.getForEntity(
                getShippingServiceUrl() + "/" + orderId + "/" + productId, 
                OrderItemDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getProductDto()).isNotNull();
        assertThat(response.getBody().getProductDto().getProductTitle()).isEqualTo("Gaming Laptop");
        assertThat(response.getBody().getOrderDto()).isNotNull();
        
        System.out.println("✅ Shipping retrieved with product and order data from external services");
    }

    @Test
    @Order(5)
    @DisplayName("Update Shipping Quantity")
    void step5_UpdateShippingQuantity() {
        OrderItemDto updateDto = OrderItemDto.builder()
                .productId(productId)
                .orderId(orderId)
                .orderedQuantity(5)
                .productDto(ProductDto.builder().productId(productId).build())
                .orderDto(OrderDto.builder().orderId(orderId).build())
                .build();

        restTemplate.put(getShippingServiceUrl(), updateDto);
        
        ResponseEntity<OrderItemDto> response = restTemplate.getForEntity(
                getShippingServiceUrl() + "/" + orderId + "/" + productId,
                OrderItemDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getOrderedQuantity()).isEqualTo(5);
        
        System.out.println("✅ Shipping quantity updated to 5");
    }
}
