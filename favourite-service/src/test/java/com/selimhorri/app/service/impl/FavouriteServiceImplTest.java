package com.selimhorri.app.service.impl;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.Favourite;
import com.selimhorri.app.domain.id.FavouriteId;
import com.selimhorri.app.dto.FavouriteDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.custom.DuplicateResourceException;
import com.selimhorri.app.exception.custom.ExternalServiceException;
import com.selimhorri.app.exception.custom.InvalidInputException;
import com.selimhorri.app.exception.custom.ResourceNotFoundException;
import com.selimhorri.app.repository.FavouriteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavouriteServiceImplTest {

    @Mock
    private FavouriteRepository favouriteRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private FavouriteServiceImpl favouriteService;

    private static final String USER_API = AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL;
    private static final String PRODUCT_API = AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL;

    private Favourite favourite(Integer userId, Integer productId) {
        Favourite fav = new Favourite();
        fav.setUserId(userId);
        fav.setProductId(productId);
        fav.setLikeDate(LocalDateTime.now());
        return fav;
    }

    private FavouriteDto favouriteDto(Integer userId, Integer productId) {
        return FavouriteDto.builder()
                .userId(userId)
                .productId(productId)
                .likeDate(LocalDateTime.now())
                .build();
    }

    private UserDto userDto(Integer id) {
        return UserDto.builder()
                .userId(id)
                .firstName("User" + id)
                .build();
    }

    private ProductDto productDto(Integer id) {
        return ProductDto.builder()
                .productId(id)
                .productTitle("Product" + id)
                .build();
    }

    private FavouriteId favouriteId(Integer userId, Integer productId) {
        return new FavouriteId(userId, productId, LocalDateTime.now());
    }

    @BeforeEach
    void resetMocks() {
        clearInvocations(favouriteRepository, restTemplate);
    }

    @Test
    @DisplayName("findAll_WhenExternalDataAvailable_EnrichesAndReturnsList")
    void findAll_WhenExternalDataAvailable_EnrichesAndReturnsList() {
        // Arrange
        when(favouriteRepository.findAll()).thenReturn(Arrays.asList(
                favourite(1, 100),
                favourite(2, 200)
        ));

        when(restTemplate.getForObject(USER_API + "/1", UserDto.class))
                .thenReturn(userDto(1));
        when(restTemplate.getForObject(USER_API + "/2", UserDto.class))
                .thenReturn(userDto(2));
        when(restTemplate.getForObject(PRODUCT_API + "/100", ProductDto.class))
                .thenReturn(productDto(100));
        when(restTemplate.getForObject(PRODUCT_API + "/200", ProductDto.class))
                .thenReturn(productDto(200));

        // Act
        List<FavouriteDto> result = favouriteService.findAll();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(dto -> {
            assertThat(dto.getUserDto()).isNotNull();
            assertThat(dto.getProductDto()).isNotNull();
        });
        verify(favouriteRepository).findAll();
    }

    @Test
    @DisplayName("findAll_WhenExternalServiceFails_FiltersOutBrokenEntries")
    void findAll_WhenExternalServiceFails_FiltersOutBrokenEntries() {
        // Arrange
        when(favouriteRepository.findAll()).thenReturn(Arrays.asList(
                favourite(1, 100),
                favourite(2, 200)
        ));

        when(restTemplate.getForObject(USER_API + "/1", UserDto.class))
                .thenReturn(userDto(1));
        when(restTemplate.getForObject(PRODUCT_API + "/100", ProductDto.class))
                .thenReturn(productDto(100));

        // Second favourite will fail
        when(restTemplate.getForObject(USER_API + "/2", UserDto.class))
                .thenThrow(new RestClientException("boom"));

        // Act
        List<FavouriteDto> result = favouriteService.findAll();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(1);
        assertThat(result.get(0).getProductId()).isEqualTo(100);
    }

    @Test
    @DisplayName("findById_WhenNotFound_ThrowsResourceNotFoundException")
    void findById_WhenNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        FavouriteId id = favouriteId(1, 100);
        when(favouriteRepository.findByUserIdAndProductId(1, 100))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> favouriteService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(favouriteRepository).findByUserIdAndProductId(1, 100);
    }

    @Test
    @DisplayName("findById_WhenUserServiceFails_ThrowsExternalServiceException")
    void findById_WhenUserServiceFails_ThrowsExternalServiceException() {
        // Arrange
        FavouriteId id = favouriteId(1, 100);
        when(favouriteRepository.findByUserIdAndProductId(1, 100))
                .thenReturn(Optional.of(favourite(1, 100)));
        when(restTemplate.getForObject(USER_API + "/1", UserDto.class))
                .thenThrow(new RestClientException("user service down"));

        // Act + Assert
        assertThatThrownBy(() -> favouriteService.findById(id))
                .isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("findById_WhenProductServiceFails_ThrowsExternalServiceException")
    void findById_WhenProductServiceFails_ThrowsExternalServiceException() {
        // Arrange
        FavouriteId id = favouriteId(1, 100);
        when(favouriteRepository.findByUserIdAndProductId(1, 100))
                .thenReturn(Optional.of(favourite(1, 100)));
        when(restTemplate.getForObject(USER_API + "/1", UserDto.class))
                .thenReturn(userDto(1));
        when(restTemplate.getForObject(PRODUCT_API + "/100", ProductDto.class))
                .thenThrow(new RestClientException("product service down"));

        // Act + Assert
        assertThatThrownBy(() -> favouriteService.findById(id))
                .isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("findById_WhenSuccess_EnrichesAndReturnsDto")
    void findById_WhenSuccess_EnrichesAndReturnsDto() {
        // Arrange
        FavouriteId id = favouriteId(1, 100);
        when(favouriteRepository.findByUserIdAndProductId(1, 100))
                .thenReturn(Optional.of(favourite(1, 100)));
        when(restTemplate.getForObject(USER_API + "/1", UserDto.class))
                .thenReturn(userDto(1));
        when(restTemplate.getForObject(PRODUCT_API + "/100", ProductDto.class))
                .thenReturn(productDto(100));

        // Act
        FavouriteDto result = favouriteService.findById(id);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(1);
        assertThat(result.getProductId()).isEqualTo(100);
        assertThat(result.getUserDto()).isNotNull();
        assertThat(result.getProductDto()).isNotNull();
    }

    @Test
    @DisplayName("save_WhenUserIdMissing_ThrowsInvalidInputException")
    void save_WhenUserIdMissing_ThrowsInvalidInputException() {
        // Arrange
        FavouriteDto request = FavouriteDto.builder()
                .userId(null)
                .productId(100)
                .build();

        // Act + Assert
        assertThatThrownBy(() -> favouriteService.save(request))
                .isInstanceOf(InvalidInputException.class);
        verifyNoInteractions(favouriteRepository);
    }

    @Test
    @DisplayName("save_WhenProductIdMissing_ThrowsInvalidInputException")
    void save_WhenProductIdMissing_ThrowsInvalidInputException() {
        // Arrange
        FavouriteDto request = FavouriteDto.builder()
                .userId(1)
                .productId(null)
                .build();

        // Act + Assert
        assertThatThrownBy(() -> favouriteService.save(request))
                .isInstanceOf(InvalidInputException.class);
        verifyNoInteractions(favouriteRepository);
    }

    @Test
    @DisplayName("save_WhenUserDoesNotExist_ThrowsResourceNotFoundException")
    void save_WhenUserDoesNotExist_ThrowsResourceNotFoundException() {
        // Arrange
        FavouriteDto request = favouriteDto(1, 100);
        when(restTemplate.getForObject(USER_API + "/1", UserDto.class))
                .thenThrow(new RestClientException("not found"));

        // Act + Assert
        assertThatThrownBy(() -> favouriteService.save(request))
                .isInstanceOf(ExternalServiceException.class);
        verifyNoInteractions(favouriteRepository);
    }

    @Test
    @DisplayName("save_WhenProductDoesNotExist_ThrowsResourceNotFoundException")
    void save_WhenProductDoesNotExist_ThrowsResourceNotFoundException() {
        // Arrange
        FavouriteDto request = favouriteDto(1, 100);
        when(restTemplate.getForObject(USER_API + "/1", UserDto.class))
                .thenReturn(userDto(1));
        when(restTemplate.getForObject(PRODUCT_API + "/100", ProductDto.class))
                .thenThrow(new RestClientException("not found"));

        // Act + Assert
        assertThatThrownBy(() -> favouriteService.save(request))
                .isInstanceOf(ExternalServiceException.class);
        verifyNoInteractions(favouriteRepository);
    }

    @Test
    @DisplayName("save_WhenFavouriteAlreadyExists_ThrowsDuplicateResourceException")
    void save_WhenFavouriteAlreadyExists_ThrowsDuplicateResourceException() {
        // Arrange
        FavouriteDto request = favouriteDto(1, 100);
        when(restTemplate.getForObject(USER_API + "/1", UserDto.class))
                .thenReturn(userDto(1));
        when(restTemplate.getForObject(PRODUCT_API + "/100", ProductDto.class))
                .thenReturn(productDto(100));
        when(favouriteRepository.existsByUserIdAndProductId(1, 100))
                .thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> favouriteService.save(request))
                .isInstanceOf(DuplicateResourceException.class);
        verify(favouriteRepository, never()).save(any());
    }

    @Test
    @DisplayName("save_WhenSuccess_SavesAndReturnsDto")
    void save_WhenSuccess_SavesAndReturnsDto() {
        // Arrange
        FavouriteDto request = favouriteDto(1, 100);
        when(restTemplate.getForObject(USER_API + "/1", UserDto.class))
                .thenReturn(userDto(1));
        when(restTemplate.getForObject(PRODUCT_API + "/100", ProductDto.class))
                .thenReturn(productDto(100));
        when(favouriteRepository.existsByUserIdAndProductId(1, 100))
                .thenReturn(false);
        when(favouriteRepository.save(any(Favourite.class)))
                .thenReturn(favourite(1, 100));

        // Act
        FavouriteDto result = favouriteService.save(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(1);
        assertThat(result.getProductId()).isEqualTo(100);
        verify(favouriteRepository).save(any(Favourite.class));
    }

    @Test
    @DisplayName("deleteById_WhenNotFound_ThrowsResourceNotFoundException")
    void deleteById_WhenNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        FavouriteId id = favouriteId(1, 100);
        when(favouriteRepository.existsByUserIdAndProductId(1, 100))
                .thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> favouriteService.deleteById(id))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(favouriteRepository, never()).deleteByUserIdAndProductId(anyInt(), anyInt());
    }

    @Test
    @DisplayName("deleteById_WhenSuccess_DeletesFavourite")
    void deleteById_WhenSuccess_DeletesFavourite() {
        // Arrange
        FavouriteId id = favouriteId(1, 100);
        when(favouriteRepository.existsByUserIdAndProductId(1, 100))
                .thenReturn(true);

        // Act
        favouriteService.deleteById(id);

        // Assert
        verify(favouriteRepository).existsByUserIdAndProductId(1, 100);
        verify(favouriteRepository).deleteByUserIdAndProductId(1, 100);
    }
}
