package com.selimhorri.app.integration;

import com.selimhorri.app.domain.Favourite;
import com.selimhorri.app.domain.id.FavouriteId;
import com.selimhorri.app.dto.FavouriteDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.custom.ExternalServiceException;
import com.selimhorri.app.exception.custom.ResourceNotFoundException;
import com.selimhorri.app.repository.FavouriteRepository;
import com.selimhorri.app.service.FavouriteService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
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
class FavouriteServiceIntegrationTest {

    @Autowired
    private FavouriteService favouriteService;

    @Autowired
    private FavouriteRepository favouriteRepository;

    @MockBean
    private RestTemplate restTemplate;

    @BeforeEach
    void setup() {
        reset(restTemplate);
        favouriteRepository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("save_WhenUserAndProductServicesReturnValidData_SavesFavouriteSuccessfully")
    void save_WhenUserAndProductServicesReturnValidData_SavesFavouriteSuccessfully() {
        // Arrange
        Integer userId = 100;
        Integer productId = 200;

        UserDto mockUser = UserDto.builder().userId(userId).firstName("John").lastName("Doe").email("john.doe@example.com").build();
        ProductDto mockProduct = ProductDto.builder().productId(productId).productTitle("Awesome Product").priceUnit(79.99).build();

        when(restTemplate.getForObject(contains("/users/"), eq(UserDto.class))).thenReturn(mockUser);
        when(restTemplate.getForObject(contains("/products/"), eq(ProductDto.class))).thenReturn(mockProduct);

        FavouriteDto favouriteDto = FavouriteDto.builder().userId(userId).productId(productId).likeDate(LocalDateTime.now()).build();

        // Act
        FavouriteDto savedFavourite = favouriteService.save(favouriteDto);

        // Assert
        assertThat(savedFavourite).isNotNull();
        assertThat(savedFavourite.getUserId()).isEqualTo(userId);
        assertThat(savedFavourite.getProductId()).isEqualTo(productId);
        assertThat(savedFavourite.getUserDto()).isNotNull();
        assertThat(savedFavourite.getProductDto()).isNotNull();
        verify(restTemplate, atLeastOnce()).getForObject(contains("/users/"), eq(UserDto.class));
        verify(restTemplate, atLeastOnce()).getForObject(contains("/products/"), eq(ProductDto.class));
    }

    @Test
    @Order(2)
    @DisplayName("save_WhenUserServiceReturns404_ThrowsResourceNotFoundException")
    void save_WhenUserServiceReturns404_ThrowsResourceNotFoundException() {
        // Arrange
        Integer nonExistentUserId = 999;
        Integer productId = 200;

        when(restTemplate.getForObject(contains("/users/" + nonExistentUserId), eq(UserDto.class)))
                .thenThrow(HttpClientErrorException.NotFound.create(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Not Found",
                        org.springframework.http.HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));
        when(restTemplate.getForObject(contains("/products/"), eq(ProductDto.class)))
                .thenReturn(ProductDto.builder().productId(productId).productTitle("Test Product").build());

        FavouriteDto favouriteDto = FavouriteDto.builder().userId(nonExistentUserId).productId(productId).likeDate(LocalDateTime.now()).build();

        // Act & Assert
        assertThatThrownBy(() -> favouriteService.save(favouriteDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User with id");
    }

    @Test
    @Order(3)
    @DisplayName("save_WhenProductServiceReturns404_ThrowsResourceNotFoundException")
    void save_WhenProductServiceReturns404_ThrowsResourceNotFoundException() {
        // Arrange
        Integer userId = 100;
        Integer nonExistentProductId = 999;

        when(restTemplate.getForObject(contains("/users/" + userId), eq(UserDto.class)))
                .thenReturn(UserDto.builder().userId(userId).firstName("Jane").lastName("Smith").build());
        when(restTemplate.getForObject(contains("/products/" + nonExistentProductId), eq(ProductDto.class)))
                .thenThrow(HttpClientErrorException.NotFound.create(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Not Found",
                        org.springframework.http.HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));

        FavouriteDto favouriteDto = FavouriteDto.builder().userId(userId).productId(nonExistentProductId).likeDate(LocalDateTime.now()).build();

        // Act & Assert
        assertThatThrownBy(() -> favouriteService.save(favouriteDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product with id");
    }

    @Test
    @Order(4)
    @DisplayName("findById_WhenBothServicesReturnData_EnrichesFavouriteWithFullDetails")
    void findById_WhenBothServicesReturnData_EnrichesFavouriteWithFullDetails() {
        // Arrange
        Integer userId = 300;
        Integer productId = 400;
        LocalDateTime likeDate = LocalDateTime.now().minusDays(1);

        favouriteRepository.save(Favourite.builder().userId(userId).productId(productId).likeDate(likeDate).build());

        UserDto mockUser = UserDto.builder().userId(userId).firstName("Alice").lastName("Johnson").email("alice.johnson@example.com").build();
        ProductDto mockProduct = ProductDto.builder().productId(productId).productTitle("Premium Gadget").priceUnit(199.99).build();

        when(restTemplate.getForObject(contains("/users/" + userId), eq(UserDto.class))).thenReturn(mockUser);
        when(restTemplate.getForObject(contains("/products/" + productId), eq(ProductDto.class))).thenReturn(mockProduct);

        // Act
        FavouriteId favouriteId = new FavouriteId(userId, productId, likeDate);
        FavouriteDto foundFavourite = favouriteService.findById(favouriteId);

        // Assert
        assertThat(foundFavourite).isNotNull();
        assertThat(foundFavourite.getUserId()).isEqualTo(userId);
        assertThat(foundFavourite.getProductId()).isEqualTo(productId);
        assertThat(foundFavourite.getUserDto()).isNotNull();
        assertThat(foundFavourite.getProductDto()).isNotNull();
        verify(restTemplate, atLeastOnce()).getForObject(contains("/users/" + userId), eq(UserDto.class));
        verify(restTemplate, atLeastOnce()).getForObject(contains("/products/" + productId), eq(ProductDto.class));
    }

    @Test
    @Order(5)
    @DisplayName("findById_WhenUserServiceUnavailable_ThrowsExternalServiceException")
    void findById_WhenUserServiceUnavailable_ThrowsExternalServiceException() {
        // Arrange
        Integer userId = 500;
        Integer productId = 600;

        favouriteRepository.save(Favourite.builder().userId(userId).productId(productId).likeDate(LocalDateTime.now()).build());

        when(restTemplate.getForObject(contains("/users/" + userId), eq(UserDto.class)))
                .thenThrow(HttpServerErrorException.ServiceUnavailable.create(
                        org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                        "Service Unavailable",
                        org.springframework.http.HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));

        // Act & Assert
        FavouriteId favouriteId = new FavouriteId(userId, productId, LocalDateTime.now());
        assertThatThrownBy(() -> favouriteService.findById(favouriteId))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("user service");
    }

    @Test
    @Order(6)
    @DisplayName("findAll_WhenMultipleFavouritesExist_EnrichesAllWithUserAndProductData")
    void findAll_WhenMultipleFavouritesExist_EnrichesAllWithUserAndProductData() {
        // Arrange
        Integer userId1 = 700;
        Integer userId2 = 701;
        Integer productId1 = 800;
        Integer productId2 = 801;

        favouriteRepository.save(Favourite.builder().userId(userId1).productId(productId1).likeDate(LocalDateTime.now().minusDays(2)).build());
        favouriteRepository.save(Favourite.builder().userId(userId2).productId(productId2).likeDate(LocalDateTime.now().minusDays(1)).build());

        UserDto mockUser1 = UserDto.builder().userId(userId1).firstName("Bob").lastName("Wilson").build();
        UserDto mockUser2 = UserDto.builder().userId(userId2).firstName("Carol").lastName("Martinez").build();
        ProductDto mockProduct1 = ProductDto.builder().productId(productId1).productTitle("Product Alpha").priceUnit(49.99).build();
        ProductDto mockProduct2 = ProductDto.builder().productId(productId2).productTitle("Product Beta").priceUnit(79.99).build();

        when(restTemplate.getForObject(contains("/users/" + userId1), eq(UserDto.class))).thenReturn(mockUser1);
        when(restTemplate.getForObject(contains("/users/" + userId2), eq(UserDto.class))).thenReturn(mockUser2);
        when(restTemplate.getForObject(contains("/products/" + productId1), eq(ProductDto.class))).thenReturn(mockProduct1);
        when(restTemplate.getForObject(contains("/products/" + productId2), eq(ProductDto.class))).thenReturn(mockProduct2);

        // Act
        List<FavouriteDto> favourites = favouriteService.findAll();

        // Assert
        assertThat(favourites).hasSize(2);
        assertThat(favourites).allMatch(fav -> fav.getUserDto() != null && fav.getProductDto() != null);
        verify(restTemplate, atLeast(4)).getForObject(anyString(), any());
    }
}
