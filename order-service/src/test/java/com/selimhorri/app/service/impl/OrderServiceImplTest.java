package com.selimhorri.app.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.selimhorri.app.domain.Cart;
import com.selimhorri.app.domain.Order;
import com.selimhorri.app.domain.enums.OrderStatus;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.exception.custom.InvalidInputException;
import com.selimhorri.app.exception.custom.InvalidOrderStatusException;
import com.selimhorri.app.exception.custom.ResourceNotFoundException;
import com.selimhorri.app.repository.CartRepository;
import com.selimhorri.app.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Cart existingCart;
    private Order existingOrderCreated;
    private Order existingOrderOrdered;
    private Order existingOrderInPayment;

    @BeforeEach
    void setUp() {
        existingCart = Cart.builder()
                .cartId(10)
                .userId(100)
                .isActive(true)
                .build();

        existingOrderCreated = Order.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now().minusDays(1))
                .orderDesc("desc")
                .orderFee(12.5)
                .cart(existingCart)
                .isActive(true)
                .status(OrderStatus.CREATED)
                .build();

        existingOrderOrdered = Order.builder()
                .orderId(2)
                .orderDate(LocalDateTime.now().minusDays(1))
                .orderDesc("desc2")
                .orderFee(20.0)
                .cart(existingCart)
                .isActive(true)
                .status(OrderStatus.ORDERED)
                .build();

        existingOrderInPayment = Order.builder()
                .orderId(3)
                .orderDate(LocalDateTime.now().minusDays(1))
                .orderDesc("desc3")
                .orderFee(33.0)
                .cart(existingCart)
                .isActive(true)
                .status(OrderStatus.IN_PAYMENT)
                .build();
    }

    @Test
    @DisplayName("save_WhenMissingCart_ThrowsInvalidInputException")
    void save_WhenMissingCart_ThrowsInvalidInputException() {
        // Arrange
        OrderDto dto = OrderDto.builder()
                .orderDesc("x")
                .orderFee(1.0)
                .build();

        // Act + Assert
        assertThrows(InvalidInputException.class, () -> orderService.save(dto));
    }

    @Test
    @DisplayName("save_WhenCartNotFound_ThrowsResourceNotFoundException")
    void save_WhenCartNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        OrderDto dto = OrderDto.builder()
                .orderDesc("x")
                .orderFee(1.0)
                .cartDto(CartDto.builder().cartId(999).build())
                .build();

        when(cartRepository.findById(999)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(ResourceNotFoundException.class, () -> orderService.save(dto));
    }

    @Test
    @DisplayName("save_WhenValid_SetsDefaultsAndPersists")
    void save_WhenValid_SetsDefaultsAndPersists() {
        // Arrange
        OrderDto dto = OrderDto.builder()
                .orderDesc("new order")
                .orderFee(50.0)
                .cartDto(CartDto.builder().cartId(10).build())
                .build();

        when(cartRepository.findById(10)).thenReturn(Optional.of(existingCart));

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            return Order.builder()
                    .orderId(100)
                    .orderDate(o.getOrderDate())
                    .orderDesc(o.getOrderDesc())
                    .orderFee(o.getOrderFee())
                    .cart(existingCart)
                    .isActive(true)
                    .status(o.getStatus())
                    .build();
        });

        // Act
        OrderDto saved = orderService.save(dto);

        // Assert
        assertThat(saved.getOrderId()).isEqualTo(100);
        assertThat(saved.getOrderStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(saved.getCartDto().getCartId()).isEqualTo(10);
    }

    @Test
    @DisplayName("updateStatus_WhenCreated_MovesToOrdered")
    void updateStatus_WhenCreated_MovesToOrdered() {
        // Arrange
        when(orderRepository.findByOrderIdAndIsActiveTrue(1)).thenReturn(Optional.of(existingOrderCreated));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        OrderDto result = orderService.updateStatus(1);

        // Assert
        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.ORDERED);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("updateStatus_WhenOrdered_MovesToInPayment")
    void updateStatus_WhenOrdered_MovesToInPayment() {
        // Arrange
        when(orderRepository.findByOrderIdAndIsActiveTrue(2)).thenReturn(Optional.of(existingOrderOrdered));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        OrderDto result = orderService.updateStatus(2);

        // Assert
        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.IN_PAYMENT);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("updateStatus_WhenInPayment_ThrowsInvalidOrderStatusException")
    void updateStatus_WhenInPayment_ThrowsInvalidOrderStatusException() {
        // Arrange
        when(orderRepository.findByOrderIdAndIsActiveTrue(3)).thenReturn(Optional.of(existingOrderInPayment));

        // Act + Assert
        assertThrows(InvalidOrderStatusException.class, () -> orderService.updateStatus(3));
    }

    @Test
    @DisplayName("deleteById_WhenInPayment_ThrowsInvalidOrderStatusException")
    void deleteById_WhenInPayment_ThrowsInvalidOrderStatusException() {
        // Arrange
        when(orderRepository.findByOrderIdAndIsActiveTrue(3)).thenReturn(Optional.of(existingOrderInPayment));

        // Act + Assert
        assertThrows(InvalidOrderStatusException.class, () -> orderService.deleteById(3));
    }

    @Test
    @DisplayName("deleteById_WhenValid_SoftDeletes")
    void deleteById_WhenValid_SoftDeletes() {
        // Arrange
        Order ok = Order.builder()
                .orderId(4)
                .orderDate(LocalDateTime.now())
                .orderDesc("desc")
                .orderFee(10.0)
                .cart(existingCart)
                .isActive(true)
                .status(OrderStatus.CREATED)
                .build();

        when(orderRepository.findByOrderIdAndIsActiveTrue(4)).thenReturn(Optional.of(ok));

        // Act
        orderService.deleteById(4);

        // Assert
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("findById_WhenMissing_ThrowsResourceNotFound")
    void findById_WhenMissing_ThrowsResourceNotFound() {
        // Arrange
        when(orderRepository.findByOrderIdAndIsActiveTrue(eq(999))).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(ResourceNotFoundException.class, () -> orderService.findById(999));
    }
}
