package com.selimhorri.e2e;

import com.selimhorri.e2e.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E Test 4: Favourite Flow - User → Product → Favourite")
public class FavouriteFlowE2ETest extends AbstractE2ETest {

    private static Integer userId;
    private static Integer productId;
    private static LocalDateTime likeDate;
    
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy__HH:mm:ss:SSSSSS");

    @Test
    @Order(1)
    @DisplayName("Create User and Product")
    void step1_CreateUserAndProduct() {
        UserDto userDto = UserDto.builder()
                .firstName("Bob")
                .lastName("Williams")
                .email("bob.w@test.com")
                .phone("5559998888")
                .build();
        ResponseEntity<UserDto> userResponse = restTemplate.postForEntity(
                getUserServiceUrl(), userDto, UserDto.class);
        userId = userResponse.getBody().getUserId();

        ProductDto productDto = ProductDto.builder()
                .productTitle("Wireless Headphones")
                .imageUrl("http://test.com/headphones.jpg")
                .sku("HEAD-001")
                .priceUnit(199.99)
                .quantity(100)
                .build();
        ResponseEntity<ProductDto> productResponse = restTemplate.postForEntity(
                getProductServiceUrl(), productDto, ProductDto.class);
        productId = productResponse.getBody().getProductId();

        System.out.println("✅ User and Product created");
    }

    @Test
    @Order(2)
    @DisplayName("User Favorites Product - Validates User and Product Services")
    void step2_CreateFavourite() {
        likeDate = LocalDateTime.now();
        FavouriteDto favouriteDto = FavouriteDto.builder()
                .userId(userId)
                .productId(productId)
                .likeDate(likeDate)
                .userDto(UserDto.builder().userId(userId).build())
                .productDto(ProductDto.builder().productId(productId).build())
                .build();

        ResponseEntity<FavouriteDto> response = restTemplate.postForEntity(
                getFavouriteServiceUrl(), favouriteDto, FavouriteDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getUserId()).isEqualTo(userId);
        assertThat(response.getBody().getProductId()).isEqualTo(productId);
        
        System.out.println("✅ Favourite created");
    }

    @Test
    @Order(3)
    @DisplayName("Retrieve Favourite with Enriched User and Product Data")
    void step3_GetFavouriteWithEnrichedData() {
        ResponseEntity<FavouriteDto> response = restTemplate.getForEntity(
                getFavouriteServiceUrl() + "/" + userId + "/" + productId + "/" + likeDate.format(formatter),
                FavouriteDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getUserDto()).isNotNull();
        assertThat(response.getBody().getProductDto()).isNotNull();
        
        System.out.println("✅ Favourite retrieved with user and product data from external services");
    }

    @Test
    @Order(4)
    @DisplayName("User Favorites Another Product")
    void step4_CreateSecondFavourite() {
        ProductDto product2Dto = ProductDto.builder()
                .productTitle("Smartphone")
                .imageUrl("http://test.com/phone.jpg")
                .sku("PHN-001")
                .priceUnit(899.99)
                .quantity(30)
                .build();
        ResponseEntity<ProductDto> productResponse = restTemplate.postForEntity(
                getProductServiceUrl(), product2Dto, ProductDto.class);
        Integer product2Id = productResponse.getBody().getProductId();
        
        LocalDateTime newLikeDate = LocalDateTime.now().plusSeconds(1);
        
        FavouriteDto favouriteDto = FavouriteDto.builder()
                .userId(userId)
                .productId(product2Id)
                .likeDate(newLikeDate)
                .userDto(UserDto.builder().userId(userId).build())
                .productDto(ProductDto.builder().productId(product2Id).build())
                .build();

        ResponseEntity<FavouriteDto> response = restTemplate.postForEntity(
                getFavouriteServiceUrl(), favouriteDto, FavouriteDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        System.out.println("✅ Second favourite created");
    }

    @Test
    @Order(5)
    @DisplayName("Retrieve All Favourites for User")
    void step5_GetAllFavourites() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                getFavouriteServiceUrl(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        
        System.out.println("✅ All favourites retrieved successfully");
    }
}