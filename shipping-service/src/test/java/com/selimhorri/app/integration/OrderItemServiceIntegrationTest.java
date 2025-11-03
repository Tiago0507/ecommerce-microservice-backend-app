package com.selimhorri.app.integration;

import com.selimhorri.app.config.TestRestTemplateConfig;
import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.OrderItem;
import com.selimhorri.app.domain.id.OrderItemId;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.OrderItemDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.exception.custom.ExternalServiceException;
import com.selimhorri.app.repository.OrderItemRepository;
import com.selimhorri.app.service.OrderItemService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for OrderItemService verifying communication with Product and Order Services.
 * Uses MockBean to simulate external service responses via RestTemplate.
 * 
 * Tests follow Arrange-Act-Assert pattern and naming convention:
 * MethodName_WhenCondition_ExpectedBehavior
 * 
 * These tests validate the integration between Shipping and external services,
 * ensuring proper handling of successful responses, errors, and edge cases.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestRestTemplateConfig.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderItemServiceIntegrationTest {

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @MockBean
    private RestTemplate restTemplate;

    private static final String PRODUCT_API = AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL;
    private static final String ORDER_API = AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL;

    @BeforeEach
    void setup() {
        reset(restTemplate);
        orderItemRepository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("findById_WhenProductAndOrderServicesReturnValidData_EnrichesOrderItemSuccessfully")
    void findById_WhenProductAndOrderServicesReturnValidData_EnrichesOrderItemSuccessfully() {
        // Arrange
        Integer productId = 100;
        Integer orderId = 200;
        OrderItemId orderItemId = new OrderItemId(productId, orderId);

        orderItemRepository.save(OrderItem.builder()
                .productId(productId)
                .orderId(orderId)
                .orderedQuantity(5)
                .build());

        ProductDto mockProduct = ProductDto.builder()
                .productId(productId)
                .productTitle("Laptop")
                .priceUnit(999.99)
                .sku("LAP-001")
                .build();

        OrderDto mockOrder = OrderDto.builder()
                .orderId(orderId)
                .orderFee(4999.95)
                .build();

        when(restTemplate.getForObject(eq(PRODUCT_API + "/" + productId), eq(ProductDto.class)))
                .thenReturn(mockProduct);
        when(restTemplate.getForObject(eq(ORDER_API + "/" + orderId), eq(OrderDto.class)))
                .thenReturn(mockOrder);

        // Act
        OrderItemDto result = orderItemService.findById(orderItemId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getProductDto()).isNotNull();
        assertThat(result.getProductDto().getProductTitle()).isEqualTo("Laptop");
        assertThat(result.getProductDto().getPriceUnit()).isEqualTo(999.99);
        assertThat(result.getOrderDto()).isNotNull();
        assertThat(result.getOrderDto().getOrderFee()).isEqualTo(4999.95);
        assertThat(result.getOrderedQuantity()).isEqualTo(5);

        verify(restTemplate, times(1)).getForObject(eq(PRODUCT_API + "/" + productId), eq(ProductDto.class));
        verify(restTemplate, times(1)).getForObject(eq(ORDER_API + "/" + orderId), eq(OrderDto.class));
    }

    @Test
    @Order(2)
    @DisplayName("findById_WhenProductServiceReturns404_ThrowsExternalServiceException")
    void findById_WhenProductServiceReturns404_ThrowsExternalServiceException() {
        // Arrange
        Integer productId = 999;
        Integer orderId = 200;
        OrderItemId orderItemId = new OrderItemId(productId, orderId);

        orderItemRepository.save(OrderItem.builder()
                .productId(productId)
                .orderId(orderId)
                .orderedQuantity(3)
                .build());

        when(restTemplate.getForObject(eq(PRODUCT_API + "/" + productId), eq(ProductDto.class)))
                .thenThrow(HttpClientErrorException.NotFound.create(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Not Found",
                        org.springframework.http.HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));

        // Act & Assert
        assertThatThrownBy(() -> orderItemService.findById(orderItemId))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Product");

        verify(restTemplate, times(1)).getForObject(eq(PRODUCT_API + "/" + productId), eq(ProductDto.class));
    }

    @Test
    @Order(3)
    @DisplayName("findById_WhenOrderServiceReturns404_ThrowsExternalServiceException")
    void findById_WhenOrderServiceReturns404_ThrowsExternalServiceException() {
        // Arrange
        Integer productId = 100;
        Integer orderId = 999;
        OrderItemId orderItemId = new OrderItemId(productId, orderId);

        orderItemRepository.save(OrderItem.builder()
                .productId(productId)
                .orderId(orderId)
                .orderedQuantity(2)
                .build());

        ProductDto mockProduct = ProductDto.builder()
                .productId(productId)
                .productTitle("Mouse")
                .priceUnit(29.99)
                .build();

        when(restTemplate.getForObject(eq(PRODUCT_API + "/" + productId), eq(ProductDto.class)))
                .thenReturn(mockProduct);
        when(restTemplate.getForObject(eq(ORDER_API + "/" + orderId), eq(OrderDto.class)))
                .thenThrow(HttpClientErrorException.NotFound.create(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Not Found",
                        org.springframework.http.HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));

        // Act & Assert
        assertThatThrownBy(() -> orderItemService.findById(orderItemId))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Order");

        verify(restTemplate, times(1)).getForObject(eq(PRODUCT_API + "/" + productId), eq(ProductDto.class));
        verify(restTemplate, times(1)).getForObject(eq(ORDER_API + "/" + orderId), eq(OrderDto.class));
    }

    @Test
    @Order(4)
    @DisplayName("findAll_WhenProductAndOrderServicesReturnValidData_EnrichesAllOrderItems")
    void findAll_WhenProductAndOrderServicesReturnValidData_EnrichesAllOrderItems() {
        // Arrange
        orderItemRepository.save(OrderItem.builder()
                .productId(100)
                .orderId(200)
                .orderedQuantity(3)
                .build());
        orderItemRepository.save(OrderItem.builder()
                .productId(101)
                .orderId(201)
                .orderedQuantity(2)
                .build());

        ProductDto mockProduct1 = ProductDto.builder()
                .productId(100)
                .productTitle("Keyboard")
                .priceUnit(79.99)
                .build();
        ProductDto mockProduct2 = ProductDto.builder()
                .productId(101)
                .productTitle("Monitor")
                .priceUnit(299.99)
                .build();

        OrderDto mockOrder1 = OrderDto.builder()
                .orderId(200)
                .orderFee(239.97)
                .build();
        OrderDto mockOrder2 = OrderDto.builder()
                .orderId(201)
                .orderFee(599.98)
                .build();

        when(restTemplate.getForObject(contains("/products/100"), eq(ProductDto.class)))
                .thenReturn(mockProduct1);
        when(restTemplate.getForObject(contains("/products/101"), eq(ProductDto.class)))
                .thenReturn(mockProduct2);
        when(restTemplate.getForObject(contains("/orders/200"), eq(OrderDto.class)))
                .thenReturn(mockOrder1);
        when(restTemplate.getForObject(contains("/orders/201"), eq(OrderDto.class)))
                .thenReturn(mockOrder2);

        // Act
        List<OrderItemDto> results = orderItemService.findAll();

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getProductDto()).isNotNull();
        assertThat(results.get(0).getOrderDto()).isNotNull();
        assertThat(results.get(1).getProductDto()).isNotNull();
        assertThat(results.get(1).getOrderDto()).isNotNull();

        verify(restTemplate, atLeast(2)).getForObject(contains("/products/"), eq(ProductDto.class));
        verify(restTemplate, atLeast(2)).getForObject(contains("/orders/"), eq(OrderDto.class));
    }

    @Test
    @Order(5)
    @DisplayName("findById_WhenProductServiceUnavailable_ThrowsExternalServiceException")
    void findById_WhenProductServiceUnavailable_ThrowsExternalServiceException() {
        // Arrange
        Integer productId = 100;
        Integer orderId = 200;
        OrderItemId orderItemId = new OrderItemId(productId, orderId);

        orderItemRepository.save(OrderItem.builder()
                .productId(productId)
                .orderId(orderId)
                .orderedQuantity(1)
                .build());

        when(restTemplate.getForObject(eq(PRODUCT_API + "/" + productId), eq(ProductDto.class)))
                .thenThrow(HttpServerErrorException.ServiceUnavailable.create(
                        org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                        "Service Unavailable",
                        org.springframework.http.HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));

        // Act & Assert
        assertThatThrownBy(() -> orderItemService.findById(orderItemId))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("product-service");

        verify(restTemplate, times(1)).getForObject(eq(PRODUCT_API + "/" + productId), eq(ProductDto.class));
    }
}
