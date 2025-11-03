package com.selimhorri.app.service.impl;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.Favourite;
import com.selimhorri.app.domain.id.FavouriteId;
import com.selimhorri.app.dto.FavouriteDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.custom.DuplicateResourceException;
import com.selimhorri.app.exception.custom.InvalidInputException;
import com.selimhorri.app.exception.custom.ResourceNotFoundException;
import com.selimhorri.app.repository.FavouriteRepository;
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
        return Favourite.builder()
                .userId(userId)
                .productId(productId)
                .likeDate(LocalDateTime.now())
                .build();
    }

    private FavouriteDto favouriteDto(Integer userId, Integer productId) {
        return FavouriteDto.builder()
                .userId(userId)
                .productId(productId)
                .likeDate(LocalDateTime.now())
                .userDto(UserDto.builder().userId(userId).build())
                .productDto(ProductDto.builder().productId(productId).build())
                .build();
    }

    private UserDto user(Integer userId, String firstName, String email) {
        return UserDto.builder()
                .userId(userId)
                .firstName(firstName)
                .email(email)
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
        clearInvocations(favouriteRepository, restTemplate);
    }

    @Test
    @DisplayName("findAll_WhenExternalServicesFetchSuccessful_EnrichesAndReturnsList")
    void findAll_WhenExternalServicesFetchSuccessful_EnrichesAndReturnsList() {
        // Arrange
        when(favouriteRepository.findAll()).thenReturn(Arrays.asList(
                favourite(1, 100),
                favourite(2, 200)
        ));

        when(restTemplate.getForObject(USER_API + "/1", UserDto.class))
                .thenReturn(user(1, "John", "john@example.com"));
        when(restTemplate.getForObject(USER_API + "/2", UserDto.class))
                .thenReturn(user(2, "Jane", "jane@example.com"));
        when(restTemplate.getForObject(PRODUCT_API + "/100", ProductDto.class))
                .thenReturn(product(100, "Product A"));
        when(restTemplate.getForObject(PRODUCT_API + "/200", ProductDto.class))
                .thenReturn(product(200, "Product B"));

        // Act
        List<FavouriteDto> result = favouriteService.findAll();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(dto -> {
            assertThat(dto.getUserDto()).isNotNull();
            assertThat(dto.getProductDto()).isNotNull();
            assertThat(dto.getUserDto().getEmail()).isNotNull();
        });
        verify(favouriteRepository).findAll();
        verify(restTemplate, times(2)).getForObject(startsWith(USER_API), eq(UserDto.class));
        verify(restTemplate, times(2)).getForObject(startsWith(PRODUCT_API), eq(ProductDto.class));
    }

    @Test
    @DisplayName("findById_WhenFavouriteExists_EnrichesAndReturnsDto")
    void findById_WhenFavouriteExists_EnrichesAndReturnsDto() {
        // Arrange
        LocalDateTime likeDate = LocalDateTime.now();
        FavouriteId id = new FavouriteId(1, 100, likeDate);
        Favourite fav = favourite(1, 100);
        fav.setLikeDate(likeDate);

        when(favouriteRepository.findById(id)).thenReturn(Optional.of(fav));
        when(restTemplate.getForObject(USER_API + "/1", UserDto.class))
                .thenReturn(user(1, "John", "john@example.com"));
        when(restTemplate.getForObject(PRODUCT_API + "/100", ProductDto.class))
                .thenReturn(product(100, "Product A"));

        // Act
        FavouriteDto result = favouriteService.findById(id);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUserDto().getUserId()).isEqualTo(1);
        assertThat(result.getProductDto().getProductId()).isEqualTo(100);
        assertThat(result.getUserDto().getEmail()).isEqualTo("john@example.com");
        verify(favouriteRepository).findById(id);
        verify(restTemplate).getForObject(USER_API + "/1", UserDto.class);
        verify(restTemplate).getForObject(PRODUCT_API + "/100", ProductDto.class);
    }

    @Test
    @DisplayName("findById_WhenFavouriteNotFound_ThrowsResourceNotFoundException")
    void findById_WhenFavouriteNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        FavouriteId id = new FavouriteId(999, 999, LocalDateTime.now());
        when(favouriteRepository.findById(id)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> favouriteService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
        verify(favouriteRepository).findById(id);
        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("save_WhenValidInput_VerifiesExternalResourcesAndSavesFavourite")
    void save_WhenValidInput_VerifiesExternalResourcesAndSavesFavourite() {
        // Arrange
        FavouriteDto input = favouriteDto(1, 100);
        FavouriteId id = new FavouriteId(1, 100, input.getLikeDate());
        when(favouriteRepository.existsById(id)).thenReturn(false);
        when(restTemplate.getForObject(USER_API + "/1", UserDto.class))
                .thenReturn(user(1, "John", "john@example.com"));
        when(restTemplate.getForObject(PRODUCT_API + "/100", ProductDto.class))
                .thenReturn(product(100, "Product A"));
        when(favouriteRepository.save(any(Favourite.class)))
                .thenReturn(favourite(1, 100));

        // Act
        FavouriteDto result = favouriteService.save(input);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(1);
        assertThat(result.getProductId()).isEqualTo(100);
        verify(favouriteRepository).existsById(id);
        verify(restTemplate).getForObject(USER_API + "/1", UserDto.class);
        verify(restTemplate).getForObject(PRODUCT_API + "/100", ProductDto.class);

        ArgumentCaptor<Favourite> captor = ArgumentCaptor.forClass(Favourite.class);
        verify(favouriteRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(1);
        assertThat(captor.getValue().getProductId()).isEqualTo(100);
    }

    @Test
    @DisplayName("save_WhenFavouriteAlreadyExists_ThrowsDuplicateResourceException")
    void save_WhenFavouriteAlreadyExists_ThrowsDuplicateResourceException() {
        // Arrange
        FavouriteDto input = favouriteDto(1, 100);
        FavouriteId id = new FavouriteId(1, 100, input.getLikeDate());
        when(favouriteRepository.existsById(id)).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> favouriteService.save(input))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
        verify(favouriteRepository).existsById(id);
        verify(favouriteRepository, never()).save(any());
        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("save_WhenUserIdIsNull_ThrowsInvalidInputException")
    void save_WhenUserIdIsNull_ThrowsInvalidInputException() {
        // Arrange
        FavouriteDto input = FavouriteDto.builder()
                .productId(100)
                .likeDate(LocalDateTime.now())
                .build();

        // Act + Assert
        assertThatThrownBy(() -> favouriteService.save(input))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Required field");
        verifyNoInteractions(favouriteRepository, restTemplate);
    }

    @Test
    @DisplayName("save_WhenProductIdIsNull_ThrowsInvalidInputException")
    void save_WhenProductIdIsNull_ThrowsInvalidInputException() {
        // Arrange
        FavouriteDto input = FavouriteDto.builder()
                .userId(1)
                .likeDate(LocalDateTime.now())
                .build();

        // Act + Assert
        assertThatThrownBy(() -> favouriteService.save(input))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Required field");
        verifyNoInteractions(favouriteRepository, restTemplate);
    }

    @Test
    @DisplayName("save_WhenUserNotFound_ThrowsInvalidInputException")
    void save_WhenUserNotFound_ThrowsInvalidInputException() {
        // Arrange
        FavouriteDto input = favouriteDto(999, 100);
        FavouriteId id = new FavouriteId(999, 100, input.getLikeDate());
        when(favouriteRepository.existsById(id)).thenReturn(false);
        when(restTemplate.getForObject(USER_API + "/999", UserDto.class))
                .thenThrow(HttpClientErrorException.NotFound.create(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Not Found", null, null, null));

        // Act + Assert
        assertThatThrownBy(() -> favouriteService.save(input))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Cannot create favourite");
        verify(favouriteRepository).existsById(id);
        verify(restTemplate).getForObject(USER_API + "/999", UserDto.class);
        verify(favouriteRepository, never()).save(any());
    }

    @Test
    @DisplayName("save_WhenProductNotFound_ThrowsInvalidInputException")
    void save_WhenProductNotFound_ThrowsInvalidInputException() {
        // Arrange
        FavouriteDto input = favouriteDto(1, 999);
        FavouriteId id = new FavouriteId(1, 999, input.getLikeDate());
        when(favouriteRepository.existsById(id)).thenReturn(false);
        when(restTemplate.getForObject(USER_API + "/1", UserDto.class))
                .thenReturn(user(1, "John", "john@example.com"));
        when(restTemplate.getForObject(PRODUCT_API + "/999", ProductDto.class))
                .thenThrow(HttpClientErrorException.NotFound.create(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Not Found", null, null, null));

        // Act + Assert
        assertThatThrownBy(() -> favouriteService.save(input))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Cannot create favourite");
        verify(favouriteRepository).existsById(id);
        verify(restTemplate).getForObject(USER_API + "/1", UserDto.class);
        verify(restTemplate).getForObject(PRODUCT_API + "/999", ProductDto.class);
        verify(favouriteRepository, never()).save(any());
    }

    @Test
    @DisplayName("save_WhenDataIntegrityViolation_ThrowsDuplicateResourceException")
    void save_WhenDataIntegrityViolation_ThrowsDuplicateResourceException() {
        // Arrange
        FavouriteDto input = favouriteDto(1, 100);
        FavouriteId id = new FavouriteId(1, 100, input.getLikeDate());
        when(favouriteRepository.existsById(id)).thenReturn(false);
        when(restTemplate.getForObject(USER_API + "/1", UserDto.class))
                .thenReturn(user(1, "John", "john@example.com"));
        when(restTemplate.getForObject(PRODUCT_API + "/100", ProductDto.class))
                .thenReturn(product(100, "Product A"));
        when(favouriteRepository.save(any(Favourite.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate key"));

        // Act + Assert
        assertThatThrownBy(() -> favouriteService.save(input))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
        verify(favouriteRepository).save(any(Favourite.class));
    }

    @Test
    @DisplayName("update_WhenFavouriteExists_UpdatesSuccessfully")
    void update_WhenFavouriteExists_UpdatesSuccessfully() {
        // Arrange
        FavouriteDto input = favouriteDto(1, 100);
        FavouriteId id = new FavouriteId(1, 100, input.getLikeDate());
        Favourite updated = favourite(1, 100);
        updated.setLikeDate(input.getLikeDate());

        when(favouriteRepository.existsById(id)).thenReturn(true);
        when(favouriteRepository.save(any(Favourite.class))).thenReturn(updated);

        // Act
        FavouriteDto result = favouriteService.update(input);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(1);
        assertThat(result.getProductId()).isEqualTo(100);
        verify(favouriteRepository).existsById(id);

        ArgumentCaptor<Favourite> captor = ArgumentCaptor.forClass(Favourite.class);
        verify(favouriteRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(1);
        assertThat(captor.getValue().getProductId()).isEqualTo(100);
    }

    @Test
    @DisplayName("update_WhenFavouriteNotFound_ThrowsResourceNotFoundException")
    void update_WhenFavouriteNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        FavouriteDto input = favouriteDto(999, 999);
        FavouriteId id = new FavouriteId(999, 999, input.getLikeDate());
        when(favouriteRepository.existsById(id)).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> favouriteService.update(input))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
        verify(favouriteRepository).existsById(id);
        verify(favouriteRepository, never()).save(any());
    }

    @Test
    @DisplayName("update_WhenUserIdIsNull_ThrowsInvalidInputException")
    void update_WhenUserIdIsNull_ThrowsInvalidInputException() {
        // Arrange
        FavouriteDto input = FavouriteDto.builder()
                .productId(100)
                .likeDate(LocalDateTime.now())
                .build();

        // Act + Assert
        assertThatThrownBy(() -> favouriteService.update(input))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Required field");
        verifyNoInteractions(favouriteRepository);
    }

    @Test
    @DisplayName("deleteById_WhenFavouriteExists_DeletesSuccessfully")
    void deleteById_WhenFavouriteExists_DeletesSuccessfully() {
        // Arrange
        FavouriteId id = new FavouriteId(1, 100, LocalDateTime.now());
        when(favouriteRepository.existsById(id)).thenReturn(true);
        doNothing().when(favouriteRepository).deleteById(id);

        // Act
        favouriteService.deleteById(id);

        // Assert
        verify(favouriteRepository).existsById(id);
        verify(favouriteRepository).deleteById(id);
    }

    @Test
    @DisplayName("deleteById_WhenFavouriteNotFound_ThrowsResourceNotFoundException")
    void deleteById_WhenFavouriteNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        FavouriteId id = new FavouriteId(999, 999, LocalDateTime.now());
        when(favouriteRepository.existsById(id)).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> favouriteService.deleteById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
        verify(favouriteRepository).existsById(id);
        verify(favouriteRepository, never()).deleteById(any());
    }
}
