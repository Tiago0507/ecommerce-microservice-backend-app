package com.selimhorri.app.service.impl;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.OrderItem;
import com.selimhorri.app.domain.id.OrderItemId;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.OrderItemDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.exception.custom.DuplicateResourceException;
import com.selimhorri.app.exception.custom.InvalidInputException;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.client.HttpClientErrorException;
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

    private OrderItem orderItem(Integer orderId, Integer productId) {
        return OrderItem.builder()
                .orderId(orderId)
                .productId(productId)
                .orderedQuantity(5)
                .build();
    }

    private OrderItemDto orderItemDto(Integer orderId, Integer productId) {
        return OrderItemDto.builder()
                .orderId(orderId)
                .productId(productId)
                .orderedQuantity(5)
                .orderDto(OrderDto.builder().orderId(orderId).build())
                .productDto(ProductDto.builder().productId(productId).build())
                .build();
    }

    private OrderDto order(Integer orderId, String desc) {
        return OrderDto.builder()
                .orderId(orderId)
                .orderDesc(desc)
                .build();
    }

    private ProductDto product(Integer productId, String title) {
        return ProductDto.builder()
                .productId(productId)
                .productTitle(title)
                .sku("SKU-" + productId)
                .build();
    }

    @BeforeEach
    void resetMocks() {
        clearInvocations(orderItemRepository, restTemplate);
    }

    @Test
    @DisplayName("findAll_WhenExternalServicesFetchSuccessful_EnrichesAndReturnsList")
    void findAll_WhenExternalServicesFetchSuccessful_EnrichesAndReturnsList() {
        // Arrange
        when(orderItemRepository.findAll()).thenReturn(Arrays.asList(
                orderItem(100, 200),
                orderItem(101, 201)
        ));

        when(restTemplate.getForObject(ORDER_API + "/100", OrderDto.class))
                .thenReturn(order(100, "ORDERED"));
        when(restTemplate.getForObject(ORDER_API + "/101", OrderDto.class))
                .thenReturn(order(101, "ORDERED"));
        when(restTemplate.getForObject(PRODUCT_API + "/200", ProductDto.class))
                .thenReturn(product(200, "Product A"));
        when(restTemplate.getForObject(PRODUCT_API + "/201", ProductDto.class))
                .thenReturn(product(201, "Product B"));

        // Act
        List<OrderItemDto> result = orderItemService.findAll();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(dto -> {
            assertThat(dto.getOrderDto()).isNotNull();
            assertThat(dto.getProductDto()).isNotNull();
            assertThat(dto.getOrderDto().getOrderDesc()).isEqualTo("ORDERED");
        });
        verify(orderItemRepository).findAll();
        verify(restTemplate, times(2)).getForObject(startsWith(ORDER_API), eq(OrderDto.class));
        verify(restTemplate, times(2)).getForObject(startsWith(PRODUCT_API), eq(ProductDto.class));
    }

    @Test
    @DisplayName("findById_WhenOrderItemExists_EnrichesAndReturnsDto")
    void findById_WhenOrderItemExists_EnrichesAndReturnsDto() {
        // Arrange
        OrderItemId id = new OrderItemId(200, 100);
        when(orderItemRepository.findById(id)).thenReturn(Optional.of(orderItem(100, 200)));
        when(restTemplate.getForObject(ORDER_API + "/100", OrderDto.class))
                .thenReturn(order(100, "ORDERED"));
        when(restTemplate.getForObject(PRODUCT_API + "/200", ProductDto.class))
                .thenReturn(product(200, "Product A"));

        // Act
        OrderItemDto result = orderItemService.findById(id);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getOrderDto().getOrderId()).isEqualTo(100);
        assertThat(result.getProductDto().getProductId()).isEqualTo(200);
        assertThat(result.getOrderDto().getOrderDesc()).isEqualTo("ORDERED");
        verify(orderItemRepository).findById(id);
        verify(restTemplate).getForObject(ORDER_API + "/100", OrderDto.class);
        verify(restTemplate).getForObject(PRODUCT_API + "/200", ProductDto.class);
    }

    @Test
    @DisplayName("findById_WhenOrderItemNotFound_ThrowsResourceNotFoundException")
    void findById_WhenOrderItemNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        OrderItemId id = new OrderItemId(999, 999);
        when(orderItemRepository.findById(id)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> orderItemService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
        verify(orderItemRepository).findById(id);
        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("save_WhenValidInput_VerifiesExternalResourcesAndSavesOrderItem")
    void save_WhenValidInput_VerifiesExternalResourcesAndSavesOrderItem() {
        // Arrange
        OrderItemDto input = orderItemDto(100, 200);
        OrderItemId id = new OrderItemId(200, 100);
        when(orderItemRepository.existsById(id)).thenReturn(false);
        when(restTemplate.getForObject(PRODUCT_API + "/200", ProductDto.class))
                .thenReturn(product(200, "Product A"));
        when(restTemplate.getForObject(ORDER_API + "/100", OrderDto.class))
                .thenReturn(order(100, "CREATED"));
        when(orderItemRepository.save(any(OrderItem.class)))
                .thenReturn(orderItem(100, 200));

        // Act
        OrderItemDto result = orderItemService.save(input);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(100);
        assertThat(result.getProductId()).isEqualTo(200);
        verify(orderItemRepository).existsById(id);
        verify(restTemplate).getForObject(PRODUCT_API + "/200", ProductDto.class);
        verify(restTemplate).getForObject(ORDER_API + "/100", OrderDto.class);

        ArgumentCaptor<OrderItem> captor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository).save(captor.capture());
        assertThat(captor.getValue().getOrderId()).isEqualTo(100);
        assertThat(captor.getValue().getProductId()).isEqualTo(200);
    }

    @Test
    @DisplayName("save_WhenOrderItemAlreadyExists_ThrowsDuplicateResourceException")
    void save_WhenOrderItemAlreadyExists_ThrowsDuplicateResourceException() {
        // Arrange
        OrderItemDto input = orderItemDto(100, 200);
        OrderItemId id = new OrderItemId(200, 100);
        when(orderItemRepository.existsById(id)).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> orderItemService.save(input))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
        verify(orderItemRepository).existsById(id);
        verify(orderItemRepository, never()).save(any());
        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("save_WhenOrderIdIsNull_ThrowsInvalidInputException")
    void save_WhenOrderIdIsNull_ThrowsInvalidInputException() {
        // Arrange
        OrderItemDto input = OrderItemDto.builder()
                .productId(200)
                .build();

        // Act + Assert
        assertThatThrownBy(() -> orderItemService.save(input))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Required field");
        verifyNoInteractions(orderItemRepository, restTemplate);
    }

    @Test
    @DisplayName("save_WhenProductIdIsNull_ThrowsInvalidInputException")
    void save_WhenProductIdIsNull_ThrowsInvalidInputException() {
        // Arrange
        OrderItemDto input = OrderItemDto.builder()
                .orderId(100)
                .build();

        // Act + Assert
        assertThatThrownBy(() -> orderItemService.save(input))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Required field");
        verifyNoInteractions(orderItemRepository, restTemplate);
    }

    @Test
    @DisplayName("save_WhenProductNotFound_ThrowsInvalidInputException")
    void save_WhenProductNotFound_ThrowsInvalidInputException() {
        // Arrange
        OrderItemDto input = orderItemDto(100, 999);
        OrderItemId id = new OrderItemId(999, 100);
        when(orderItemRepository.existsById(id)).thenReturn(false);
        when(restTemplate.getForObject(PRODUCT_API + "/999", ProductDto.class))
                .thenThrow(HttpClientErrorException.NotFound.create(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Not Found", null, null, null));

        // Act + Assert
        assertThatThrownBy(() -> orderItemService.save(input))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Product");
        verify(orderItemRepository).existsById(id);
        verify(restTemplate).getForObject(PRODUCT_API + "/999", ProductDto.class);
        verify(orderItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("save_WhenOrderNotFound_ThrowsInvalidInputException")
    void save_WhenOrderNotFound_ThrowsInvalidInputException() {
        // Arrange
        OrderItemDto input = orderItemDto(999, 200);
        OrderItemId id = new OrderItemId(200, 999);
        when(orderItemRepository.existsById(id)).thenReturn(false);
        when(restTemplate.getForObject(PRODUCT_API + "/200", ProductDto.class))
                .thenReturn(product(200, "Product A"));
        when(restTemplate.getForObject(ORDER_API + "/999", OrderDto.class))
                .thenThrow(HttpClientErrorException.NotFound.create(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Not Found", null, null, null));

        // Act + Assert
        assertThatThrownBy(() -> orderItemService.save(input))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Order");
        verify(orderItemRepository).existsById(id);
        verify(restTemplate).getForObject(PRODUCT_API + "/200", ProductDto.class);
        verify(restTemplate).getForObject(ORDER_API + "/999", OrderDto.class);
        verify(orderItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("save_WhenDataIntegrityViolation_ThrowsDuplicateResourceException")
    void save_WhenDataIntegrityViolation_ThrowsDuplicateResourceException() {
        // Arrange
        OrderItemDto input = orderItemDto(100, 200);
        OrderItemId id = new OrderItemId(200, 100);
        when(orderItemRepository.existsById(id)).thenReturn(false);
        when(restTemplate.getForObject(PRODUCT_API + "/200", ProductDto.class))
                .thenReturn(product(200, "Product A"));
        when(restTemplate.getForObject(ORDER_API + "/100", OrderDto.class))
                .thenReturn(order(100, "CREATED"));
        when(orderItemRepository.save(any(OrderItem.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate key"));

        // Act + Assert
        assertThatThrownBy(() -> orderItemService.save(input))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
        verify(orderItemRepository).save(any(OrderItem.class));
    }

    @Test
    @DisplayName("update_WhenOrderItemExists_UpdatesSuccessfully")
    void update_WhenOrderItemExists_UpdatesSuccessfully() {
        // Arrange
        OrderItemDto input = orderItemDto(100, 200);
        input.setOrderedQuantity(10);
        OrderItemId id = new OrderItemId(200, 100);
        OrderItem updated = orderItem(100, 200);
        updated.setOrderedQuantity(10);

        when(orderItemRepository.existsById(id)).thenReturn(true);
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(updated);

        // Act
        OrderItemDto result = orderItemService.update(input);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getOrderedQuantity()).isEqualTo(10);
        verify(orderItemRepository).existsById(id);

        ArgumentCaptor<OrderItem> captor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository).save(captor.capture());
        assertThat(captor.getValue().getOrderedQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("update_WhenOrderItemNotFound_ThrowsResourceNotFoundException")
    void update_WhenOrderItemNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        OrderItemDto input = orderItemDto(999, 999);
        OrderItemId id = new OrderItemId(999, 999);
        when(orderItemRepository.existsById(id)).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> orderItemService.update(input))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
        verify(orderItemRepository).existsById(id);
        verify(orderItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("update_WhenOrderIdIsNull_ThrowsInvalidInputException")
    void update_WhenOrderIdIsNull_ThrowsInvalidInputException() {
        // Arrange
        OrderItemDto input = OrderItemDto.builder()
                .productId(200)
                .orderedQuantity(10)
                .build();

        // Act + Assert
        assertThatThrownBy(() -> orderItemService.update(input))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Required field");
        verifyNoInteractions(orderItemRepository);
    }

    @Test
    @DisplayName("deleteById_WhenOrderItemExists_DeletesSuccessfully")
    void deleteById_WhenOrderItemExists_DeletesSuccessfully() {
        // Arrange
        OrderItemId id = new OrderItemId(200, 100);
        when(orderItemRepository.existsById(id)).thenReturn(true);
        doNothing().when(orderItemRepository).deleteById(id);

        // Act
        orderItemService.deleteById(id);

        // Assert
        verify(orderItemRepository).existsById(id);
        verify(orderItemRepository).deleteById(id);
    }

    @Test
    @DisplayName("deleteById_WhenOrderItemNotFound_ThrowsResourceNotFoundException")
    void deleteById_WhenOrderItemNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        OrderItemId id = new OrderItemId(999, 999);
        when(orderItemRepository.existsById(id)).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> orderItemService.deleteById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
        verify(orderItemRepository).existsById(id);
        verify(orderItemRepository, never()).deleteById(any());
    }
}
