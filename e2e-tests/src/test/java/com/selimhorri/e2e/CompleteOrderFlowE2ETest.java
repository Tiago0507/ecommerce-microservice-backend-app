package com.selimhorri.e2e;

import com.selimhorri.e2e.dto.CartDto;
import com.selimhorri.e2e.dto.OrderDto;
import com.selimhorri.e2e.dto.UserDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E Test 1: Complete Order Flow - User → Cart → Order")
public class CompleteOrderFlowE2ETest extends AbstractE2ETest {

    private static Integer userId;
    private static Integer cartId;
    private static Integer orderId;

    @Test
    @Order(1)
    @DisplayName("Create User in User Service")
    void step1_CreateUser() {
        UserDto userDto = UserDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@test.com")
                .phone("1234567890")
                .build();

        ResponseEntity<UserDto> response = restTemplate.postForEntity(
                getUserServiceUrl(), userDto, UserDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUserId()).isNotNull();
        
        userId = response.getBody().getUserId();
        System.out.println("✅ User created: ID = " + userId);
    }

    @Test
    @Order(2)
    @DisplayName("Create Cart for User - Validates User Service Communication")
    void step2_CreateCart() {
        CartDto cartDto = CartDto.builder()
                .userId(userId)
                .build();

        ResponseEntity<CartDto> response = restTemplate.postForEntity(
                getCartServiceUrl(), cartDto, CartDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCartId()).isNotNull();
        assertThat(response.getBody().getUserDto()).isNotNull();
        assertThat(response.getBody().getUserDto().getFirstName()).isEqualTo("John");
        
        cartId = response.getBody().getCartId();
        System.out.println("✅ Cart created with enriched user data: Cart ID = " + cartId);
    }

    @Test
    @Order(3)
    @DisplayName("Create Order from Cart")
    void step3_CreateOrder() {
        OrderDto orderDto = OrderDto.builder()
                .orderDesc("E2E Test Order")
                .orderFee(199.99)
                .cartDto(CartDto.builder().cartId(cartId).build())
                .build();

        ResponseEntity<OrderDto> response = restTemplate.postForEntity(
                getOrderServiceUrl(), orderDto, OrderDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getOrderId()).isNotNull();
        
        orderId = response.getBody().getOrderId();
        System.out.println("✅ Order created: ID = " + orderId);
    }

    @Test
    @Order(4)
    @DisplayName("Retrieve Order with Enriched Cart and User Data")
    void step4_VerifyOrderWithUserData() {
        ResponseEntity<OrderDto> response = restTemplate.getForEntity(
                getOrderServiceUrl() + "/" + orderId, OrderDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCartDto()).isNotNull();
        assertThat(response.getBody().getCartDto().getUserDto()).isNotNull();
        assertThat(response.getBody().getCartDto().getUserDto().getFirstName()).isEqualTo("John");
        
        System.out.println("✅ Order retrieved with complete user data from User Service");
    }

    @Test
    @Order(5)
    @DisplayName("Update Order Status to ORDERED")
    void step5_UpdateOrderStatus() {
        OrderDto updateDto = OrderDto.builder()
                .orderId(orderId)
                .orderDesc("E2E Test Order")
                .orderFee(199.99)
                .orderStatus("ORDERED")
                .cartDto(CartDto.builder().cartId(cartId).build())
                .build();

        restTemplate.put(getOrderServiceUrl(), updateDto);
        
        ResponseEntity<OrderDto> response = restTemplate.getForEntity(
                getOrderServiceUrl() + "/" + orderId, OrderDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getOrderStatus()).isEqualTo("ORDERED");
        
        System.out.println("✅ Order status updated to ORDERED");
    }
}
