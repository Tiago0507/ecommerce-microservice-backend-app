package com.selimhorri.app.integration;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.Cart;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.custom.ExternalServiceException;
import com.selimhorri.app.exception.custom.ResourceNotFoundException;
import com.selimhorri.app.repository.CartRepository;
import com.selimhorri.app.service.CartService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CartServiceIntegrationTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private CartRepository cartRepository;

    @MockBean
    private RestTemplate restTemplate;

    @BeforeEach
    void setup() {
        reset(restTemplate);
        cartRepository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("save_WhenUserServiceReturnsValidUser_SavesCartSuccessfully")
    void save_WhenUserServiceReturnsValidUser_SavesCartSuccessfully() {
        // Arrange
        UserDto mockUser = UserDto.builder()
                .userId(1)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("1234567890")
                .build();

        when(restTemplate.getForObject(anyString(), eq(UserDto.class)))
                .thenReturn(mockUser);

        CartDto toSave = CartDto.builder().userId(1).build();

        // Act
        CartDto saved = cartService.save(toSave);

        // Assert
        assertThat(saved).isNotNull();
        assertThat(saved.getCartId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(1);
        verify(restTemplate, atLeastOnce()).getForObject(anyString(), eq(UserDto.class));
    }

    @Test
    @Order(2)
    @DisplayName("save_WhenUserServiceReturns404_ThrowsResourceNotFoundException")
    void save_WhenUserServiceReturns404_ThrowsResourceNotFoundException() {
        // Arrange
        when(restTemplate.getForObject(anyString(), eq(UserDto.class)))
                .thenThrow(HttpClientErrorException.NotFound.create(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Not Found",
                        org.springframework.http.HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));

        CartDto toSave = CartDto.builder().userId(999).build();

        // Act & Assert
        assertThatThrownBy(() -> cartService.save(toSave))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User with id");
    }

    @Test
    @Order(3)
    @DisplayName("findById_WhenUserServiceReturnsUser_EnrichesCartWithUserData")
    void findById_WhenUserServiceReturnsUser_EnrichesCartWithUserData() {
        // Arrange: create an ACTIVE cart directly via repository
        Cart activeCart = Cart.builder()
                .userId(1)
                .isActive(true)
                .build();
        activeCart = cartRepository.save(activeCart);

        UserDto mockUser = UserDto.builder()
                .userId(1)
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .phone("9876543210")
                .build();

        when(restTemplate.getForObject(contains(AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/"), eq(UserDto.class)))
                .thenReturn(mockUser);

        // Act
        CartDto found = cartService.findById(activeCart.getCartId());

        // Assert
        assertThat(found).isNotNull();
        assertThat(found.getUserDto()).isNotNull();
        assertThat(found.getUserDto().getFirstName()).isEqualTo("Jane");
        assertThat(found.getUserDto().getLastName()).isEqualTo("Smith");
        verify(restTemplate, atLeastOnce()).getForObject(anyString(), eq(UserDto.class));
    }

    @Test
    @Order(4)
    @DisplayName("findById_WhenUserServiceUnavailable_ThrowsExternalServiceException")
    void findById_WhenUserServiceUnavailable_ThrowsExternalServiceException() {
        // Arrange: create an ACTIVE cart
        Cart activeCart = Cart.builder()
                .userId(1)
                .isActive(true)
                .build();
        Integer cartId = cartRepository.save(activeCart).getCartId();

        when(restTemplate.getForObject(anyString(), eq(UserDto.class)))
                .thenThrow(HttpServerErrorException.ServiceUnavailable.create(
                        org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                        "Service Unavailable",
                        org.springframework.http.HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));

        // Act & Assert
        assertThatThrownBy(() -> cartService.findById(cartId))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("user-service");
    }

    @Test
    @Order(5)
    @DisplayName("findAll_WhenUserServiceReturnsMultipleUsers_EnrichesAllCartsSuccessfully")
    void findAll_WhenUserServiceReturnsMultipleUsers_EnrichesAllCartsSuccessfully() {
        // Arrange: create ACTIVE carts
        cartRepository.save(Cart.builder().userId(1).isActive(true).build());
        cartRepository.save(Cart.builder().userId(2).isActive(true).build());

        UserDto mockUser1 = UserDto.builder().userId(1).firstName("Alice").lastName("Johnson").build();
        UserDto mockUser2 = UserDto.builder().userId(2).firstName("Bob").lastName("Williams").build();

        when(restTemplate.getForObject(contains("/users/1"), eq(UserDto.class))).thenReturn(mockUser1);
        when(restTemplate.getForObject(contains("/users/2"), eq(UserDto.class))).thenReturn(mockUser2);

        // Act
        List<CartDto> carts = cartService.findAll();

        // Assert
        assertThat(carts).hasSize(2);
        assertThat(carts.get(0).getUserDto()).isNotNull();
        assertThat(carts.get(1).getUserDto()).isNotNull();
        verify(restTemplate, atLeast(2)).getForObject(anyString(), eq(UserDto.class));
    }
}
