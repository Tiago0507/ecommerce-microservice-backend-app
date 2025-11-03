package com.selimhorri.app.integration;

import com.selimhorri.app.config.TestRestTemplateConfig;
import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.Favourite;
import com.selimhorri.app.domain.id.FavouriteId;
import com.selimhorri.app.dto.FavouriteDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.custom.ExternalServiceException;
import com.selimhorri.app.repository.FavouriteRepository;
import com.selimhorri.app.service.FavouriteService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for FavouriteService verifying communication with User and Product Services.
 * Uses MockBean to simulate external service responses via RestTemplate.
 * 
 * Tests follow Arrange-Act-Assert pattern and naming convention:
 * MethodName_WhenCondition_ExpectedBehavior
 * 
 * These tests validate the integration between Favourite and external services,
 * ensuring proper handling of successful responses, errors, and edge cases.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestRestTemplateConfig.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FavouriteServiceIntegrationTest {

    @Autowired
    private FavouriteService favouriteService;

    @Autowired
    private FavouriteRepository favouriteRepository;

    @MockBean
    private RestTemplate restTemplate;

    private static final String USER_API = AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL;
    private static final String PRODUCT_API = AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL;

    @BeforeEach
    void setup() {
        reset(restTemplate);
        favouriteRepository.deleteAll();
    }

    private Favourite createFavourite(Integer userId, Integer productId, LocalDateTime likeDate) {
        Favourite favourite = new Favourite();
        favourite.setUserId(userId);
        favourite.setProductId(productId);
        favourite.setLikeDate(likeDate);
        favourite.setCreatedAt(Instant.now());
        favourite.setUpdatedAt(Instant.now());
        return favourite;
    }

    @Test
    @Order(1)
    @DisplayName("findById_WhenUserAndProductServicesReturnValidData_EnrichesFavouriteSuccessfully")
    void findById_WhenUserAndProductServicesReturnValidData_EnrichesFavouriteSuccessfully() {
        // Arrange
        Integer userId = 1;
        Integer productId = 100;
        LocalDateTime likeDate = LocalDateTime.of(2024, 1, 15, 10, 30);
        FavouriteId favouriteId = new FavouriteId(userId, productId, likeDate);

        favouriteRepository.save(createFavourite(userId, productId, likeDate));

        UserDto mockUser = UserDto.builder()
                .userId(userId)
                .firstName("Alice")
                .lastName("Johnson")
                .email("alice@example.com")
                .phone("555-1234")
                .build();

        ProductDto mockProduct = ProductDto.builder()
                .productId(productId)
                .productTitle("Wireless Mouse")
                .priceUnit(25.99)
                .sku("WM-001")
                .build();

        when(restTemplate.getForObject(eq(USER_API + "/" + userId), eq(UserDto.class)))
                .thenReturn(mockUser);
        when(restTemplate.getForObject(eq(PRODUCT_API + "/" + productId), eq(ProductDto.class)))
                .thenReturn(mockProduct);

        // Act
        FavouriteDto result = favouriteService.findById(favouriteId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUserDto()).isNotNull();
        assertThat(result.getUserDto().getFirstName()).isEqualTo("Alice");
        assertThat(result.getUserDto().getEmail()).isEqualTo("alice@example.com");
        assertThat(result.getProductDto()).isNotNull();
        assertThat(result.getProductDto().getProductTitle()).isEqualTo("Wireless Mouse");
        assertThat(result.getProductDto().getPriceUnit()).isEqualTo(25.99);

        verify(restTemplate, times(1)).getForObject(eq(USER_API + "/" + userId), eq(UserDto.class));
        verify(restTemplate, times(1)).getForObject(eq(PRODUCT_API + "/" + productId), eq(ProductDto.class));
    }

    @Test
    @Order(2)
    @DisplayName("findById_WhenUserServiceReturns404_ThrowsExternalServiceException")
    void findById_WhenUserServiceReturns404_ThrowsExternalServiceException() {
        // Arrange
        Integer userId = 999;
        Integer productId = 100;
        LocalDateTime likeDate = LocalDateTime.of(2024, 1, 15, 10, 30);
        FavouriteId favouriteId = new FavouriteId(userId, productId, likeDate);

        favouriteRepository.save(createFavourite(userId, productId, likeDate));

        when(restTemplate.getForObject(eq(USER_API + "/" + userId), eq(UserDto.class)))
                .thenThrow(HttpClientErrorException.NotFound.create(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Not Found",
                        org.springframework.http.HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));

        // Act & Assert
        assertThatThrownBy(() -> favouriteService.findById(favouriteId))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("User");

        verify(restTemplate, times(1)).getForObject(eq(USER_API + "/" + userId), eq(UserDto.class));
    }

    @Test
    @Order(3)
    @DisplayName("findById_WhenProductServiceReturns404_ThrowsExternalServiceException")
    void findById_WhenProductServiceReturns404_ThrowsExternalServiceException() {
        // Arrange
        Integer userId = 1;
        Integer productId = 999;
        LocalDateTime likeDate = LocalDateTime.of(2024, 1, 15, 10, 30);
        FavouriteId favouriteId = new FavouriteId(userId, productId, likeDate);

        favouriteRepository.save(createFavourite(userId, productId, likeDate));

        UserDto mockUser = UserDto.builder()
                .userId(userId)
                .firstName("Bob")
                .lastName("Smith")
                .email("bob@example.com")
                .build();

        when(restTemplate.getForObject(eq(USER_API + "/" + userId), eq(UserDto.class)))
                .thenReturn(mockUser);
        when(restTemplate.getForObject(eq(PRODUCT_API + "/" + productId), eq(ProductDto.class)))
                .thenThrow(HttpClientErrorException.NotFound.create(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Not Found",
                        org.springframework.http.HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));

        // Act & Assert
        assertThatThrownBy(() -> favouriteService.findById(favouriteId))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Product");

        verify(restTemplate, times(1)).getForObject(eq(USER_API + "/" + userId), eq(UserDto.class));
        verify(restTemplate, times(1)).getForObject(eq(PRODUCT_API + "/" + productId), eq(ProductDto.class));
    }

    @Test
    @Order(4)
    @DisplayName("findAll_WhenUserAndProductServicesReturnValidData_EnrichesAllFavourites")
    void findAll_WhenUserAndProductServicesReturnValidData_EnrichesAllFavourites() {
        // Arrange
        LocalDateTime likeDate1 = LocalDateTime.of(2024, 1, 10, 9, 0);
        LocalDateTime likeDate2 = LocalDateTime.of(2024, 1, 11, 14, 30);

        favouriteRepository.save(createFavourite(1, 100, likeDate1));
        favouriteRepository.save(createFavourite(2, 101, likeDate2));

        UserDto mockUser1 = UserDto.builder()
                .userId(1)
                .firstName("Carol")
                .lastName("White")
                .email("carol@example.com")
                .build();
        UserDto mockUser2 = UserDto.builder()
                .userId(2)
                .firstName("Dave")
                .lastName("Brown")
                .email("dave@example.com")
                .build();

        ProductDto mockProduct1 = ProductDto.builder()
                .productId(100)
                .productTitle("Mechanical Keyboard")
                .priceUnit(89.99)
                .build();
        ProductDto mockProduct2 = ProductDto.builder()
                .productId(101)
                .productTitle("Gaming Headset")
                .priceUnit(59.99)
                .build();

        when(restTemplate.getForObject(contains("/users/1"), eq(UserDto.class)))
                .thenReturn(mockUser1);
        when(restTemplate.getForObject(contains("/users/2"), eq(UserDto.class)))
                .thenReturn(mockUser2);
        when(restTemplate.getForObject(contains("/products/100"), eq(ProductDto.class)))
                .thenReturn(mockProduct1);
        when(restTemplate.getForObject(contains("/products/101"), eq(ProductDto.class)))
                .thenReturn(mockProduct2);

        // Act
        List<FavouriteDto> results = favouriteService.findAll();

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getUserDto()).isNotNull();
        assertThat(results.get(0).getProductDto()).isNotNull();
        assertThat(results.get(1).getUserDto()).isNotNull();
        assertThat(results.get(1).getProductDto()).isNotNull();

        verify(restTemplate, atLeast(2)).getForObject(contains("/users/"), eq(UserDto.class));
        verify(restTemplate, atLeast(2)).getForObject(contains("/products/"), eq(ProductDto.class));
    }

    @Test
    @Order(5)
    @DisplayName("findById_WhenUserServiceUnavailable_ThrowsExternalServiceException")
    void findById_WhenUserServiceUnavailable_ThrowsExternalServiceException() {
        // Arrange
        Integer userId = 1;
        Integer productId = 100;
        LocalDateTime likeDate = LocalDateTime.of(2024, 1, 15, 10, 30);
        FavouriteId favouriteId = new FavouriteId(userId, productId, likeDate);

        favouriteRepository.save(createFavourite(userId, productId, likeDate));

        when(restTemplate.getForObject(eq(USER_API + "/" + userId), eq(UserDto.class)))
                .thenThrow(HttpServerErrorException.ServiceUnavailable.create(
                        org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                        "Service Unavailable",
                        org.springframework.http.HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));

        // Act & Assert
        assertThatThrownBy(() -> favouriteService.findById(favouriteId))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("user-service");

        verify(restTemplate, times(1)).getForObject(eq(USER_API + "/" + userId), eq(UserDto.class));
    }
}
