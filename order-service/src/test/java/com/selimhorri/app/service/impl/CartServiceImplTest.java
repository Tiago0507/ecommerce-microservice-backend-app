package com.selimhorri.app.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.domain.Cart;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.custom.ExternalServiceException;
import com.selimhorri.app.exception.custom.InvalidInputException;
import com.selimhorri.app.exception.custom.ResourceNotFoundException;
import com.selimhorri.app.repository.CartRepository;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private CartServiceImpl cartService;

    private Cart cart(int id, int userId, boolean active) {
        return Cart.builder().cartId(id).userId(userId).isActive(active).build();
    }

    @Test
    @DisplayName("findAll_WhenUsersResolved_EnrichesAndReturnsAll")
    void findAll_WhenUsersResolved_EnrichesAndReturnsAll() {
        // Arrange
        when(cartRepository.findAllByIsActiveTrue())
                .thenReturn(Arrays.asList(cart(1, 10, true), cart(2, 20, true)));
        when(restTemplate.getForObject(org.mockito.ArgumentMatchers.contains("/10"), eq(UserDto.class)))
                .thenReturn(UserDto.builder().userId(10).firstName("A").build());
        when(restTemplate.getForObject(org.mockito.ArgumentMatchers.contains("/20"), eq(UserDto.class)))
                .thenReturn(UserDto.builder().userId(20).firstName("B").build());

        // Act
        List<CartDto> result = cartService.findAll();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUserDto()).isNotNull();
        assertThat(result.get(1).getUserDto()).isNotNull();
    }

    @Test
    @DisplayName("findAll_WhenRestClientException_FiltersOutThatCart")
    void findAll_WhenRestClientException_FiltersOutThatCart() {
        // Arrange
        when(cartRepository.findAllByIsActiveTrue()).thenReturn(Arrays.asList(cart(1, 10, true)));
        when(restTemplate.getForObject(org.mockito.ArgumentMatchers.anyString(), eq(UserDto.class)))
                .thenThrow(new RestClientException("boom"));

        // Act
        List<CartDto> result = cartService.findAll();

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findById_WhenCartFoundAndUserResolved_ReturnsDtoWithUser")
    void findById_WhenCartFoundAndUserResolved_ReturnsDtoWithUser() {
        // Arrange
        when(cartRepository.findByCartIdAndIsActiveTrue(5)).thenReturn(Optional.of(cart(5, 55, true)));
        when(restTemplate.getForObject(org.mockito.ArgumentMatchers.contains("/55"), eq(UserDto.class)))
                .thenReturn(UserDto.builder().userId(55).firstName("John").build());

        // Act
        CartDto dto = cartService.findById(5);

        // Assert
        assertThat(dto.getCartId()).isEqualTo(5);
        assertThat(dto.getUserDto()).isNotNull();
        assertThat(dto.getUserDto().getUserId()).isEqualTo(55);
    }

    @Test
    @DisplayName("findById_WhenRestClientException_ThrowsExternalServiceException")
    void findById_WhenRestClientException_ThrowsExternalServiceException() {
        // Arrange
        when(cartRepository.findByCartIdAndIsActiveTrue(7)).thenReturn(Optional.of(cart(7, 77, true)));
        when(restTemplate.getForObject(org.mockito.ArgumentMatchers.anyString(), eq(UserDto.class)))
                .thenThrow(new RestClientException("boom"));

        // Act + Assert
        assertThrows(ExternalServiceException.class, () -> cartService.findById(7));
    }

    @Test
    @DisplayName("findById_WhenCartMissing_ThrowsResourceNotFound")
    void findById_WhenCartMissing_ThrowsResourceNotFound() {
        // Arrange
        when(cartRepository.findByCartIdAndIsActiveTrue(404)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(ResourceNotFoundException.class, () -> cartService.findById(404));
    }

    @Test
    @DisplayName("save_WhenUserIdMissing_ThrowsInvalidInputException")
    void save_WhenUserIdMissing_ThrowsInvalidInputException() {
        // Arrange
        CartDto dto = CartDto.builder().cartId(1).userId(null).build();

        // Act + Assert
        assertThrows(InvalidInputException.class, () -> cartService.save(dto));
        verify(cartRepository, never()).save(any());
    }

    @Test
    @DisplayName("save_WhenRestClientException_ThrowsExternalServiceException")
    void save_WhenRestClientException_ThrowsExternalServiceException() {
        // Arrange
        CartDto dto = CartDto.builder().userId(456).build();
        when(restTemplate.getForObject(org.mockito.ArgumentMatchers.contains("/456"), eq(UserDto.class)))
                .thenThrow(new RestClientException("boom"));

        // Act + Assert
        assertThrows(ExternalServiceException.class, () -> cartService.save(dto));
        verify(cartRepository, never()).save(any());
    }

    @Test
    @DisplayName("save_WhenValid_PersistsAndReturnsMappedDto")
    void save_WhenValid_PersistsAndReturnsMappedDto() {
        // Arrange
        CartDto dto = CartDto.builder().userId(42).build();
        when(restTemplate.getForObject(org.mockito.ArgumentMatchers.contains("/42"), eq(UserDto.class)))
                .thenReturn(UserDto.builder().userId(42).firstName("Jane").build());
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> {
            Cart c = inv.getArgument(0);
            return Cart.builder().cartId(900).userId(c.getUserId()).isActive(true).build();
        });

        // Act
        CartDto saved = cartService.save(dto);

        // Assert
        assertThat(saved.getCartId()).isEqualTo(900);
        assertThat(saved.getUserId()).isEqualTo(42);

        ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
        verify(cartRepository).save(captor.capture());
        assertThat(captor.getValue().getCartId()).isNull();
    }

    @Test
    @DisplayName("deleteById_WhenNotFound_ThrowsResourceNotFoundException")
    void deleteById_WhenNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        when(cartRepository.findById(111)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(ResourceNotFoundException.class, () -> cartService.deleteById(111));
        verify(cartRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteById_WhenFound_SetsInactiveAndSaves")
    void deleteById_WhenFound_SetsInactiveAndSaves() {
        // Arrange
        Cart existing = cart(321, 9, true);
        when(cartRepository.findById(321)).thenReturn(Optional.of(existing));

        // Act
        cartService.deleteById(321);

        // Assert
        verify(cartRepository, times(1)).save(any(Cart.class));
    }
}
