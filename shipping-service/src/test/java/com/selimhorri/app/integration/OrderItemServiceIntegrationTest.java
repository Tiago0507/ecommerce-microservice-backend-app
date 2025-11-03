package com.selimhorri.app.integration;

import com.selimhorri.app.domain.OrderItem;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.OrderItemDto;
import com.selimhorri.app.dto.OrderStatus;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.exception.custom.ExternalServiceException;
import com.selimhorri.app.exception.custom.ResourceNotFoundException;
import com.selimhorri.app.repository.OrderItemRepository;
import com.selimhorri.app.service.OrderItemService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for OrderItemService (Shipping Service) verifying communication 
 * with Order Service and Product Service.
 * Uses @MockBean(RestTemplate) to simulate external service responses.
 * 
 * Tests follow Arrange-Act-Assert pattern and naming convention:
 * MethodName_WhenCondition_ExpectedBehavior
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderItemServiceIntegrationTest {

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private OrderItemRepository orderItemRepository;

        @MockBean
        private RestTemplate restTemplate;

    @BeforeEach
    void setup() {
                reset(restTemplate);
        orderItemRepository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("save_WhenOrderAndProductServicesReturnValidData_SavesOrderItemSuccessfully")
    void save_WhenOrderAndProductServicesReturnValidData_SavesOrderItemSuccessfully() {
        // Arrange
        Integer orderId = 100;
        Integer productId = 200;

        // For save flow, order must be in CREATED status
        OrderDto mockOrder = OrderDto.builder()
                .orderId(orderId)
                .orderStatus(OrderStatus.CREATED.name())
                .orderFee(99.99)
                .orderDesc("Test order")
                .build();

        ProductDto mockProduct = ProductDto.builder()
                .productId(productId)
                .productTitle("Test Product")
                .sku("TEST-SKU-001")
                .priceUnit(49.99)
                .quantity(100)
                .build();

        when(restTemplate.getForObject(contains("/orders/" + orderId), eq(OrderDto.class)))
                .thenReturn(mockOrder);
        when(restTemplate.getForObject(contains("/products/" + productId), eq(ProductDto.class)))
                .thenReturn(mockProduct);
        when(restTemplate.patchForObject(contains("/orders/" + orderId + "/status"), any(), eq(Void.class)))
                .thenReturn(null);

        OrderItemDto orderItemDto = OrderItemDto.builder()
                .orderId(orderId)
                .productId(productId)
                .orderedQuantity(2)
                .build();

        // Act
        OrderItemDto savedOrderItem = orderItemService.save(orderItemDto);

                // Assert
        assertThat(savedOrderItem).isNotNull();
        assertThat(savedOrderItem.getOrderId()).isEqualTo(orderId);
        assertThat(savedOrderItem.getProductId()).isEqualTo(productId);
        assertThat(savedOrderItem.getOrderDto()).isNotNull();
        assertThat(savedOrderItem.getOrderDto().getOrderId()).isEqualTo(orderId);
        assertThat(savedOrderItem.getProductDto()).isNotNull();
        assertThat(savedOrderItem.getProductDto().getProductId()).isEqualTo(productId);
                verify(restTemplate, atLeastOnce()).getForObject(contains("/orders/" + orderId), eq(OrderDto.class));
                verify(restTemplate, atLeastOnce()).getForObject(contains("/products/" + productId), eq(ProductDto.class));
                verify(restTemplate, atLeastOnce()).patchForObject(contains("/orders/" + orderId + "/status"), any(), eq(Void.class));
    }

    @Test
    @Order(2)
    @DisplayName("save_WhenOrderServiceReturns404_ThrowsResourceNotFoundException")
    void save_WhenOrderServiceReturns404_ThrowsResourceNotFoundException() {
        // Arrange
        Integer nonExistentOrderId = 999;
        Integer productId = 200;

        when(restTemplate.getForObject(contains("/orders/" + nonExistentOrderId), eq(OrderDto.class)))
                .thenThrow(HttpClientErrorException.NotFound.create(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Not Found",
                        org.springframework.http.HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));
        when(restTemplate.getForObject(contains("/products/" + productId), eq(ProductDto.class)))
                .thenReturn(ProductDto.builder().productId(productId).productTitle("Test Product").quantity(10).build());

        OrderItemDto orderItemDto = OrderItemDto.builder()
                .orderId(nonExistentOrderId)
                .productId(productId)
                .orderedQuantity(1)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> orderItemService.save(orderItemDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order with id");
    }

    @Test
    @Order(3)
    @DisplayName("save_WhenProductServiceReturns404_ThrowsResourceNotFoundException")
    void save_WhenProductServiceReturns404_ThrowsResourceNotFoundException() {
        // Arrange
        Integer orderId = 100;
        Integer nonExistentProductId = 999;

        when(restTemplate.getForObject(contains("/orders/" + orderId), eq(OrderDto.class)))
                .thenReturn(OrderDto.builder().orderId(orderId).orderStatus(OrderStatus.CREATED.name()).build());
        when(restTemplate.getForObject(contains("/products/" + nonExistentProductId), eq(ProductDto.class)))
                .thenThrow(HttpClientErrorException.NotFound.create(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Not Found",
                        org.springframework.http.HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));

        OrderItemDto orderItemDto = OrderItemDto.builder()
                .orderId(orderId)
                .productId(nonExistentProductId)
                .orderedQuantity(1)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> orderItemService.save(orderItemDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product with id");
    }

    @Test
    @Order(4)
    @DisplayName("findById_WhenBothServicesReturnData_EnrichesOrderItemWithFullDetails")
    void findById_WhenBothServicesReturnData_EnrichesOrderItemWithFullDetails() {
        // Arrange
        Integer orderId = 300;
        Integer productId = 400;

        OrderItem savedOrderItem = orderItemRepository.save(OrderItem.builder()
                .orderId(orderId)
                .productId(productId)
                .orderedQuantity(3)
                .isActive(true)
                .build());

        OrderDto mockOrder = OrderDto.builder()
                .orderId(orderId)
                .orderStatus(OrderStatus.ORDERED.name())
                .orderFee(199.99)
                .orderDesc("Priority shipping")
                .build();

        ProductDto mockProduct = ProductDto.builder()
                .productId(productId)
                .productTitle("Premium Product")
                .sku("PREM-SKU-002")
                .priceUnit(66.66)
                .quantity(50)
                .build();

        when(restTemplate.getForObject(contains("/orders/" + orderId), eq(OrderDto.class)))
                .thenReturn(mockOrder);
        when(restTemplate.getForObject(contains("/products/" + productId), eq(ProductDto.class)))
                .thenReturn(mockProduct);

        // Act
        OrderItemDto foundOrderItem = orderItemService.findById(savedOrderItem.getOrderId());

        // Assert
        assertThat(foundOrderItem).isNotNull();
        assertThat(foundOrderItem.getOrderId()).isEqualTo(savedOrderItem.getOrderId());
        assertThat(foundOrderItem.getProductId()).isEqualTo(savedOrderItem.getProductId());
        assertThat(foundOrderItem.getOrderDto()).isNotNull();
        assertThat(foundOrderItem.getOrderDto().getOrderId()).isEqualTo(orderId);
                assertThat(foundOrderItem.getOrderDto().getOrderStatus()).isEqualTo(OrderStatus.ORDERED.name());
        assertThat(foundOrderItem.getProductDto()).isNotNull();
        assertThat(foundOrderItem.getProductDto().getProductId()).isEqualTo(productId);
        assertThat(foundOrderItem.getProductDto().getProductTitle()).isEqualTo("Premium Product");
        assertThat(foundOrderItem.getProductDto().getSku()).isEqualTo("PREM-SKU-002");
                verify(restTemplate, atLeastOnce()).getForObject(contains("/orders/" + orderId), eq(OrderDto.class));
                verify(restTemplate, atLeastOnce()).getForObject(contains("/products/" + productId), eq(ProductDto.class));
    }

    @Test
    @Order(5)
    @DisplayName("findById_WhenProductServiceUnavailable_ThrowsExternalServiceException")
    void findById_WhenProductServiceUnavailable_ThrowsExternalServiceException() {
        // Arrange
        Integer orderId = 500;
        Integer productId = 600;

        OrderItem savedOrderItem = orderItemRepository.save(OrderItem.builder()
                .orderId(orderId)
                .productId(productId)
                .orderedQuantity(1)
                .isActive(true)
                .build());

        when(restTemplate.getForObject(contains("/orders/" + orderId), eq(OrderDto.class)))
                .thenReturn(OrderDto.builder().orderId(orderId).orderStatus(OrderStatus.ORDERED.name()).build());
        when(restTemplate.getForObject(contains("/products/" + productId), eq(ProductDto.class)))
                .thenThrow(HttpServerErrorException.ServiceUnavailable.create(
                        org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                        "Service Unavailable",
                        org.springframework.http.HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));

        // Act & Assert
        assertThatThrownBy(() -> orderItemService.findById(savedOrderItem.getOrderId()))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Product information not available");
    }

    @Test
    @Order(6)
    @DisplayName("findAll_WhenMultipleOrderItemsExist_EnrichesAllWithOrderAndProductData")
    void findAll_WhenMultipleOrderItemsExist_EnrichesAllWithOrderAndProductData() {
        // Arrange
        Integer orderId1 = 700;
        Integer orderId2 = 701;
        Integer productId1 = 800;
        Integer productId2 = 801;

        orderItemRepository.save(OrderItem.builder()
                .orderId(orderId1)
                .productId(productId1)
                .orderedQuantity(2)
                .isActive(true)
                .build());

        orderItemRepository.save(OrderItem.builder()
                .orderId(orderId2)
                .productId(productId2)
                .orderedQuantity(5)
                .isActive(true)
                .build());

        OrderDto mockOrder1 = OrderDto.builder()
                .orderId(orderId1)
                .orderStatus(OrderStatus.ORDERED.name())
                .orderFee(99.99)
                .build();

        OrderDto mockOrder2 = OrderDto.builder()
                .orderId(orderId2)
                .orderStatus(OrderStatus.ORDERED.name())
                .orderFee(149.99)
                .build();

        ProductDto mockProduct1 = ProductDto.builder()
                .productId(productId1)
                .productTitle("Product A")
                .priceUnit(49.99)
                .build();

        ProductDto mockProduct2 = ProductDto.builder()
                .productId(productId2)
                .productTitle("Product B")
                .priceUnit(29.99)
                .build();

        when(restTemplate.getForObject(contains("/orders/" + orderId1), eq(OrderDto.class)))
                .thenReturn(mockOrder1);
        when(restTemplate.getForObject(contains("/orders/" + orderId2), eq(OrderDto.class)))
                .thenReturn(mockOrder2);
        when(restTemplate.getForObject(contains("/products/" + productId1), eq(ProductDto.class)))
                .thenReturn(mockProduct1);
        when(restTemplate.getForObject(contains("/products/" + productId2), eq(ProductDto.class)))
                .thenReturn(mockProduct2);

        // Act
        var orderItems = orderItemService.findAll();

        // Assert
        assertThat(orderItems).hasSize(2);
        assertThat(orderItems).allMatch(item -> 
            item.getOrderDto() != null && item.getProductDto() != null);
        assertThat(orderItems).extracting(i -> i.getProductDto().getProductTitle())
                .containsExactlyInAnyOrder("Product A", "Product B");

                verify(restTemplate, atLeast(4)).getForObject(anyString(), any());
    }
}
