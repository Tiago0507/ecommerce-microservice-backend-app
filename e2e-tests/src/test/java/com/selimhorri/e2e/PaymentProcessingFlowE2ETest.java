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
@DisplayName("E2E Test 2: Payment Processing Flow - Order → Payment → Status Update")
public class PaymentProcessingFlowE2ETest extends AbstractE2ETest {

    private static Integer userId;
    private static Integer cartId;
    private static Integer orderId;
    private static Integer paymentId;

    @Test
    @Order(1)
    @DisplayName("Setup: Create User, Cart, and Order with ORDERED Status")
    void step1_SetupOrderForPayment() {
        // Create User
        UserDto userDto = UserDto.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@test.com")
                .phone("9876543210")
                .build();
        ResponseEntity<UserDto> userResponse = restTemplate.postForEntity(
                getUserServiceUrl(), userDto, UserDto.class);
        userId = userResponse.getBody().getUserId();

        // Create Cart
        CartDto cartDto = CartDto.builder().userId(userId).build();
        ResponseEntity<CartDto> cartResponse = restTemplate.postForEntity(
                getCartServiceUrl(), cartDto, CartDto.class);
        cartId = cartResponse.getBody().getCartId();

        // Create Order with ORDERED status
        OrderDto orderDto = OrderDto.builder()
                .orderDesc("Order for payment test")
                .orderFee(299.99)
                .orderStatus("ORDERED")
                .cartDto(CartDto.builder().cartId(cartId).build())
                .build();
        ResponseEntity<OrderDto> orderResponse = restTemplate.postForEntity(
                getOrderServiceUrl(), orderDto, OrderDto.class);
        orderId = orderResponse.getBody().getOrderId();

        System.out.println("✅ Setup complete: User " + userId + ", Order " + orderId);
    }

    @Test
    @Order(2)
    @DisplayName("Process Payment - Validates Order Service Communication")
    void step2_ProcessPayment() {
        PaymentDto paymentDto = PaymentDto.builder()
                .isPayed(true)
                .paymentStatus("COMPLETED")
                .orderDto(OrderDto.builder().orderId(orderId).build())
                .build();

        ResponseEntity<PaymentDto> response = restTemplate.postForEntity(
                getPaymentServiceUrl(), paymentDto, PaymentDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getPaymentId()).isNotNull();
        assertThat(response.getBody().getIsPayed()).isTrue();
        
        paymentId = response.getBody().getPaymentId();
        System.out.println("✅ Payment processed: ID = " + paymentId);
    }

    @Test
    @Order(3)
    @DisplayName("Verify Order Status Updated to DELIVERED by Payment Service")
    void step3_VerifyOrderStatusUpdated() {
        ResponseEntity<OrderDto> response = restTemplate.getForEntity(
                getOrderServiceUrl() + "/" + orderId, OrderDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getOrderStatus()).isEqualTo("DELIVERED");
        
        System.out.println("✅ Order status automatically updated to DELIVERED by Payment Service");
    }

    @Test
    @Order(4)
    @DisplayName("Retrieve Payment with Enriched Order Data")
    void step4_GetPaymentWithOrderData() {
        ResponseEntity<PaymentDto> response = restTemplate.getForEntity(
                getPaymentServiceUrl() + "/" + paymentId, PaymentDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getOrderDto()).isNotNull();
        assertThat(response.getBody().getOrderDto().getOrderFee()).isEqualTo(299.99);
        
        System.out.println("✅ Payment retrieved with enriched order data from Order Service");
    }

    @Test
    @Order(5)
    @DisplayName("Update Payment to REFUNDED")
    void step5_RefundPayment() {
        PaymentDto refundDto = PaymentDto.builder()
                .paymentId(paymentId)
                .isPayed(false)
                .paymentStatus("REFUNDED")
                .orderDto(OrderDto.builder().orderId(orderId).build())
                .build();

        restTemplate.put(getPaymentServiceUrl(), refundDto);
        
        ResponseEntity<PaymentDto> response = restTemplate.getForEntity(
                getPaymentServiceUrl() + "/" + paymentId, PaymentDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getPaymentStatus()).isEqualTo("REFUNDED");
        
        System.out.println("✅ Payment refunded successfully");
    }
}
