package com.selimhorri.app.service.impl;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.OrderItem;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.OrderItemDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.exception.custom.ExternalServiceException;
import com.selimhorri.app.exception.custom.InvalidInputException;
import com.selimhorri.app.exception.custom.InvalidOrderStatusException;
import com.selimhorri.app.exception.custom.ResourceNotFoundException;
import com.selimhorri.app.repository.OrderItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderItemServiceImplTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private OrderItemServiceImpl orderItemService;

    private static final String ORDER_API = AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL;
    private static final String PRODUCT_API = AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL;

    private OrderItem orderItem(Integer orderId, Integer productId, Integer quantity, boolean active) {
        return OrderItem.builder()
                .orderId(orderId)
                .productId(productId)
                .orderedQuantity(quantity)
                .isActive(active)
                .build();
    }

    private OrderItemDto orderItemDto(Integer orderId, Integer productId, Integer quantity) {
        return OrderItemDto.builder()
                .orderId(orderId)
                .productId(productId)
                .orderedQuantity(quantity)
                .orderDto(OrderDto.builder().orderId(orderId).build())
                .productDto(ProductDto.builder().productId(productId).build())
                .build();
    }

    private OrderDto orderDto(Integer orderId, String status) {
        return OrderDto.builder()
                .orderId(orderId)
                .orderStatus(status)
                .build();
    }

    private ProductDto productDto(Integer productId, Integer quantity) {
        return ProductDto.builder()
                .productId(productId)
                .productTitle("Product" + productId)
                .quantity(quantity)
                .build();
    }

    @BeforeEach
    void resetMocks() {
        clearInvocations(orderItemRepository, restTemplate);
    }

    @Test
    @DisplayName("findAll_WhenExternalDataAvailable_EnrichesAndReturnsActiveItems")
    void findAll_WhenExternalDataAvailable_EnrichesAndReturnsActiveItems() {
        // Arrange
        when(orderItemRepository.findByIsActiveTrue()).thenReturn(Arrays.asList(
                orderItem(10, 100, 5, true),
                orderItem(20, 200, 3, true)
        ));

        when(restTemplate.getForObject(PRODUCT_API + "/100", ProductDto.class))
                .thenReturn(productDto(100, 50));
        when(restTemplate.getForObject(ORDER_API + "/10", OrderDto.class))
                .thenReturn(orderDto(10, "ORDERED"));

        when(restTemplate.getForObject(PRODUCT_API + "/200", ProductDto.class))
                .thenReturn(productDto(200, 30));
        when(restTemplate.getForObject(ORDER_API + "/20", OrderDto.class))
                .thenReturn(orderDto(20, "ORDERED"));

        // Act
        List<OrderItemDto> result = orderItemService.findAll();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(dto -> {
            assertThat(dto.getProductDto()).isNotNull();
            assertThat(dto.getOrderDto()).isNotNull();
            assertThat(dto.getOrderDto().getOrderStatus()).isEqualTo("ORDERED");
        });
    }

    @Test
    @DisplayName("findAll_WhenOrderNotInOrderedStatus_FiltersOutItem")
    void findAll_WhenOrderNotInOrderedStatus_FiltersOutItem() {
        // Arrange
        when(orderItemRepository.findByIsActiveTrue()).thenReturn(Arrays.asList(
                orderItem(10, 100, 5, true),
                orderItem(20, 200, 3, true)
        ));

        when(restTemplate.getForObject(PRODUCT_API + "/100", ProductDto.class))
                .thenReturn(productDto(100, 50));
        when(restTemplate.getForObject(ORDER_API + "/10", OrderDto.class))
                .thenReturn(orderDto(10, "ORDERED"));

        when(restTemplate.getForObject(PRODUCT_API + "/200", ProductDto.class))
                .thenReturn(productDto(200, 30));
        when(restTemplate.getForObject(ORDER_API + "/20", OrderDto.class))
                .thenReturn(orderDto(20, "CREATED")); // Not ORDERED

        // Act
        List<OrderItemDto> result = orderItemService.findAll();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrderId()).isEqualTo(10);
    }

    @Test
    @DisplayName("findAllByOrderId_WhenSuccess_ReturnsFilteredList")
    void findAllByOrderId_WhenSuccess_ReturnsFilteredList() {
        // Arrange
        when(orderItemRepository.findAllByOrderIdAndIsActiveTrue(10)).thenReturn(Arrays.asList(
                orderItem(10, 100, 5, true),
                orderItem(10, 101, 2, true)
        ));

        when(restTemplate.getForObject(PRODUCT_API + "/100", ProductDto.class))
                .thenReturn(productDto(100, 50));
        when(restTemplate.getForObject(ORDER_API + "/10", OrderDto.class))
                .thenReturn(orderDto(10, "ORDERED"));

        when(restTemplate.getForObject(PRODUCT_API + "/101", ProductDto.class))
                .thenReturn(productDto(101, 30));

        // Act
        List<OrderItemDto> result = orderItemService.findAllByOrderId(10);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(dto -> assertThat(dto.getOrderId()).isEqualTo(10));
    }

    @Test
    @DisplayName("findById_WhenNotFoundOrInactive_ThrowsResourceNotFoundException")
    void findById_WhenNotFoundOrInactive_ThrowsResourceNotFoundException() {
        // Arrange
        when(orderItemRepository.findById(999))
                .thenReturn(Optional.of(orderItem(10, 100, 5, false))); // inactive

        // Act + Assert
        assertThatThrownBy(() -> orderItemService.findById(999))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("findById_WhenProductServiceFails_ThrowsExternalServiceException")
    void findById_WhenProductServiceFails_ThrowsExternalServiceException() {
        // Arrange
        when(orderItemRepository.findById(1))
                .thenReturn(Optional.of(orderItem(10, 100, 5, true)));
        when(restTemplate.getForObject(PRODUCT_API + "/100", ProductDto.class))
                .thenThrow(new RestClientException("product service down"));

        // Act + Assert
        assertThatThrownBy(() -> orderItemService.findById(1))
                .isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("findById_WhenOrderNotInOrderedStatus_ThrowsResourceNotFoundException")
    void findById_WhenOrderNotInOrderedStatus_ThrowsResourceNotFoundException() {
        // Arrange
        when(orderItemRepository.findById(1))
                .thenReturn(Optional.of(orderItem(10, 100, 5, true)));
        when(restTemplate.getForObject(PRODUCT_API + "/100", ProductDto.class))
                .thenReturn(productDto(100, 50));
        when(restTemplate.getForObject(ORDER_API + "/10", OrderDto.class))
                .thenReturn(orderDto(10, "CREATED")); // Not ORDERED

        // Act + Assert
        assertThatThrownBy(() -> orderItemService.findById(1))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("findById_WhenSuccess_EnrichesAndReturnsDto")
    void findById_WhenSuccess_EnrichesAndReturnsDto() {
        // Arrange
        when(orderItemRepository.findById(1))
                .thenReturn(Optional.of(orderItem(10, 100, 5, true)));
        when(restTemplate.getForObject(PRODUCT_API + "/100", ProductDto.class))
                .thenReturn(productDto(100, 50));
        when(restTemplate.getForObject(ORDER_API + "/10", OrderDto.class))
                .thenReturn(orderDto(10, "ORDERED"));

        // Act
        OrderItemDto result = orderItemService.findById(1);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(10);
        assertThat(result.getProductId()).isEqualTo(100);
        assertThat(result.getProductDto()).isNotNull();
        assertThat(result.getOrderDto()).isNotNull();
    }

    @Test
    @DisplayName("findByOrderIdAndProductId_WhenNotFound_ThrowsResourceNotFoundException")
    void findByOrderIdAndProductId_WhenNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        when(orderItemRepository.findByOrderIdAndProductIdAndIsActiveTrue(10, 100))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> orderItemService.findByOrderIdAndProductId(10, 100))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("findByOrderIdAndProductId_WhenSuccess_EnrichesAndReturnsDto")
    void findByOrderIdAndProductId_WhenSuccess_EnrichesAndReturnsDto() {
        // Arrange
        when(orderItemRepository.findByOrderIdAndProductIdAndIsActiveTrue(10, 100))
                .thenReturn(Optional.of(orderItem(10, 100, 5, true)));
        when(restTemplate.getForObject(PRODUCT_API + "/100", ProductDto.class))
                .thenReturn(productDto(100, 50));
        when(restTemplate.getForObject(ORDER_API + "/10", OrderDto.class))
                .thenReturn(orderDto(10, "ORDERED"));

        // Act
        OrderItemDto result = orderItemService.findByOrderIdAndProductId(10, 100);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(10);
        assertThat(result.getProductId()).isEqualTo(100);
    }

    @Test
    @DisplayName("save_WhenOrderIdMissing_ThrowsInvalidInputException")
    void save_WhenOrderIdMissing_ThrowsInvalidInputException() {
        // Arrange
        OrderItemDto request = OrderItemDto.builder()
                .orderId(null)
                .productId(100)
                .orderedQuantity(5)
                .build();

        // Act + Assert
        assertThatThrownBy(() -> orderItemService.save(request))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    @DisplayName("save_WhenProductIdMissing_ThrowsInvalidInputException")
    void save_WhenProductIdMissing_ThrowsInvalidInputException() {
        // Arrange
        OrderItemDto request = OrderItemDto.builder()
                .orderId(10)
                .productId(null)
                .orderedQuantity(5)
                .build();

        // Act + Assert
        assertThatThrownBy(() -> orderItemService.save(request))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    @DisplayName("save_WhenQuantityMissing_ThrowsInvalidInputException")
    void save_WhenQuantityMissing_ThrowsInvalidInputException() {
        // Arrange
        OrderItemDto request = OrderItemDto.builder()
                .orderId(10)
                .productId(100)
                .orderedQuantity(null)
                .build();

        // Act + Assert
        assertThatThrownBy(() -> orderItemService.save(request))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    @DisplayName("save_WhenOrderNotInCreatedStatus_ThrowsInvalidOrderStatusException")
    void save_WhenOrderNotInCreatedStatus_ThrowsInvalidOrderStatusException() {
        // Arrange
        OrderItemDto request = orderItemDto(10, 100, 5);
        when(restTemplate.getForObject(ORDER_API + "/10", OrderDto.class))
                .thenReturn(orderDto(10, "ORDERED")); // Not CREATED

        // Act + Assert
        assertThatThrownBy(() -> orderItemService.save(request))
                .isInstanceOf(InvalidOrderStatusException.class);
    }

    @Test
    @DisplayName("save_WhenInsufficientStock_ThrowsInvalidInputException")
    void save_WhenInsufficientStock_ThrowsInvalidInputException() {
        // Arrange
        OrderItemDto request = orderItemDto(10, 100, 50);
        when(restTemplate.getForObject(ORDER_API + "/10", OrderDto.class))
                .thenReturn(orderDto(10, "CREATED"));
        when(restTemplate.getForObject(PRODUCT_API + "/100", ProductDto.class))
                .thenReturn(productDto(100, 10)); // Only 10 available, requesting 50

        // Act + Assert
        assertThatThrownBy(() -> orderItemService.save(request))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    @DisplayName("save_WhenOrderServiceUnavailable_ThrowsExternalServiceException")
    void save_WhenOrderServiceUnavailable_ThrowsExternalServiceException() {
        // Arrange
        OrderItemDto request = orderItemDto(10, 100, 5);
        when(restTemplate.getForObject(ORDER_API + "/10", OrderDto.class))
                .thenThrow(new RestClientException("order service down"));

        // Act + Assert
        assertThatThrownBy(() -> orderItemService.save(request))
                .isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("save_WhenSuccess_SavesAndUpdatesOrderStatus")
    void save_WhenSuccess_SavesAndUpdatesOrderStatus() {
        // Arrange
        OrderItemDto request = orderItemDto(10, 100, 5);
        when(restTemplate.getForObject(ORDER_API + "/10", OrderDto.class))
                .thenReturn(orderDto(10, "CREATED"));
        when(restTemplate.getForObject(PRODUCT_API + "/100", ProductDto.class))
                .thenReturn(productDto(100, 50));
        when(orderItemRepository.save(any(OrderItem.class)))
                .thenReturn(orderItem(10, 100, 5, true));

        // Act
        OrderItemDto result = orderItemService.save(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(10);
        assertThat(result.getProductId()).isEqualTo(100);
        verify(restTemplate).patchForObject(ORDER_API + "/10/status", null, Void.class);
        verify(orderItemRepository).save(any(OrderItem.class));
    }

    @Test
    @DisplayName("deleteById_WhenNotFound_ThrowsResourceNotFoundException")
    void deleteById_WhenNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        when(orderItemRepository.findByOrderIdAndIsActiveTrue(999))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> orderItemService.deleteById(999))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deleteById_WhenOrderNotInOrderedStatus_ThrowsInvalidOrderStatusException")
    void deleteById_WhenOrderNotInOrderedStatus_ThrowsInvalidOrderStatusException() {
        // Arrange
        when(orderItemRepository.findByOrderIdAndIsActiveTrue(1))
                .thenReturn(Optional.of(orderItem(10, 100, 5, true)));
        when(restTemplate.getForObject(ORDER_API + "/10", OrderDto.class))
                .thenReturn(orderDto(10, "CREATED")); // Not ORDERED

        // Act + Assert
        assertThatThrownBy(() -> orderItemService.deleteById(1))
                .isInstanceOf(InvalidOrderStatusException.class);
    }

    @Test
    @DisplayName("deleteById_WhenSuccess_SoftDeletesItem")
    void deleteById_WhenSuccess_SoftDeletesItem() {
        // Arrange
        OrderItem item = orderItem(10, 100, 5, true);
        when(orderItemRepository.findByOrderIdAndIsActiveTrue(1))
                .thenReturn(Optional.of(item));
        when(restTemplate.getForObject(ORDER_API + "/10", OrderDto.class))
                .thenReturn(orderDto(10, "ORDERED"));
        when(orderItemRepository.save(any(OrderItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        orderItemService.deleteById(1);

        // Assert
        ArgumentCaptor<OrderItem> captor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
    }

    @Test
    @DisplayName("deleteByOrderIdAndProductId_WhenNotFound_ThrowsResourceNotFoundException")
    void deleteByOrderIdAndProductId_WhenNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        when(orderItemRepository.findByOrderIdAndProductIdAndIsActiveTrue(10, 100))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> orderItemService.deleteByOrderIdAndProductId(10, 100))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deleteByOrderIdAndProductId_WhenSuccess_SoftDeletesItem")
    void deleteByOrderIdAndProductId_WhenSuccess_SoftDeletesItem() {
        // Arrange
        OrderItem item = orderItem(10, 100, 5, true);
        when(orderItemRepository.findByOrderIdAndProductIdAndIsActiveTrue(10, 100))
                .thenReturn(Optional.of(item));
        when(restTemplate.getForObject(ORDER_API + "/10", OrderDto.class))
                .thenReturn(orderDto(10, "ORDERED"));
        when(orderItemRepository.save(any(OrderItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        orderItemService.deleteByOrderIdAndProductId(10, 100);

        // Assert
        ArgumentCaptor<OrderItem> captor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
    }
}
