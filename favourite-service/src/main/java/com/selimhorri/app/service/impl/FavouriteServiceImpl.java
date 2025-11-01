package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.id.FavouriteId;
import com.selimhorri.app.dto.FavouriteDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.ErrorCode;
import com.selimhorri.app.exception.custom.DuplicateResourceException;
import com.selimhorri.app.exception.custom.ExternalServiceException;
import com.selimhorri.app.exception.custom.InvalidInputException;
import com.selimhorri.app.exception.custom.ResourceNotFoundException;
import com.selimhorri.app.helper.FavouriteMappingHelper;
import com.selimhorri.app.repository.FavouriteRepository;
import com.selimhorri.app.service.FavouriteService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class FavouriteServiceImpl implements FavouriteService {

    private final FavouriteRepository favouriteRepository;
    private final RestTemplate restTemplate;

    @Override
    public List<FavouriteDto> findAll() {
        log.info("Fetching all favourites");
        
        return this.favouriteRepository.findAll()
                .stream()
                .map(FavouriteMappingHelper::map)
                .map(this::enrichWithExternalDataSafely)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public FavouriteDto findById(final FavouriteId favouriteId) {
        log.info("Fetching favourite by userId: {} and productId: {}", 
                favouriteId.getUserId(), favouriteId.getProductId());
        
        FavouriteDto favouriteDto = this.favouriteRepository
                .findByUserIdAndProductId(favouriteId.getUserId(), favouriteId.getProductId())
                .map(FavouriteMappingHelper::map)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.FAVOURITE_NOT_FOUND, 
                        favouriteId.getUserId(), 
                        favouriteId.getProductId()));

        enrichWithUserData(favouriteDto);
        enrichWithProductData(favouriteDto);
        
        return favouriteDto;
    }

    @Override
    public FavouriteDto save(final FavouriteDto favouriteDto) {
        log.info("Creating favourite for user: {} and product: {}", 
                favouriteDto.getUserId(), favouriteDto.getProductId());
        
        validateFavouriteInput(favouriteDto);
        verifyUserExists(favouriteDto.getUserId());
        verifyProductExists(favouriteDto.getProductId());
        verifyFavouriteNotExists(favouriteDto.getUserId(), favouriteDto.getProductId());
        
        return FavouriteMappingHelper.map(
                this.favouriteRepository.save(FavouriteMappingHelper.map(favouriteDto)));
    }

    @Override
    @Transactional
    public void deleteById(FavouriteId favouriteId) {
        log.info("Deleting favourite by userId: {} and productId: {}", 
                favouriteId.getUserId(), favouriteId.getProductId());
        
        if (!favouriteRepository.existsByUserIdAndProductId(
                favouriteId.getUserId(), favouriteId.getProductId())) {
            throw new ResourceNotFoundException(
                    ErrorCode.FAVOURITE_NOT_FOUND, 
                    favouriteId.getUserId(), 
                    favouriteId.getProductId());
        }
        
        favouriteRepository.deleteByUserIdAndProductId(
                favouriteId.getUserId(), favouriteId.getProductId());
    }

    private FavouriteDto enrichWithExternalDataSafely(FavouriteDto favouriteDto) {
        try {
            UserDto userDto = fetchUser(favouriteDto.getUserId());
            ProductDto productDto = fetchProduct(favouriteDto.getProductId());
            
            if (userDto == null || productDto == null) {
                log.warn("User {} or product {} not found, excluding favourite", 
                        favouriteDto.getUserId(), favouriteDto.getProductId());
                return null;
            }
            
            favouriteDto.setUserDto(userDto);
            favouriteDto.setProductDto(productDto);
            return favouriteDto;
            
        } catch (Exception e) {
            log.warn("Error fetching details for favourite (user: {}, product: {}), excluding: {}",
                    favouriteDto.getUserId(), favouriteDto.getProductId(), e.getMessage());
            return null;
        }
    }

    private void enrichWithUserData(FavouriteDto favouriteDto) {
        try {
            UserDto userDto = fetchUser(favouriteDto.getUserId());
            if (userDto == null) {
                throw new ResourceNotFoundException(
                        ErrorCode.USER_NOT_FOUND, favouriteDto.getUserId());
            }
            favouriteDto.setUserDto(userDto);
            
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException(
                    ErrorCode.USER_NOT_FOUND, favouriteDto.getUserId());
        } catch (RestClientException e) {
            log.error("Error fetching user {}: {}", 
                    favouriteDto.getUserId(), e.getMessage());
            throw new ExternalServiceException(
                    "Failed to communicate with user service", e);
        }
    }

    private void enrichWithProductData(FavouriteDto favouriteDto) {
        try {
            ProductDto productDto = fetchProduct(favouriteDto.getProductId());
            if (productDto == null) {
                throw new ResourceNotFoundException(
                        ErrorCode.PRODUCT_NOT_FOUND, favouriteDto.getProductId());
            }
            favouriteDto.setProductDto(productDto);
            
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException(
                    ErrorCode.PRODUCT_NOT_FOUND, favouriteDto.getProductId());
        } catch (RestClientException e) {
            log.error("Error fetching product {}: {}", 
                    favouriteDto.getProductId(), e.getMessage());
            throw new ExternalServiceException(
                    "Failed to communicate with product service", e);
        }
    }

    private void validateFavouriteInput(FavouriteDto favouriteDto) {
        if (favouriteDto.getUserId() == null) {
            throw new InvalidInputException(
                    ErrorCode.MISSING_REQUIRED_FIELD, "User ID is required");
        }
        if (favouriteDto.getProductId() == null) {
            throw new InvalidInputException(
                    ErrorCode.MISSING_REQUIRED_FIELD, "Product ID is required");
        }
    }

    private void verifyUserExists(Integer userId) {
        try {
            UserDto userDto = fetchUser(userId);
            if (userDto == null) {
                throw new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, userId);
            }
            
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, userId);
        } catch (RestClientException e) {
            log.error("Error communicating with user service for user {}: {}", 
                    userId, e.getMessage());
            throw new ExternalServiceException(
                    "Error communicating with user service", e);
        }
    }

    private void verifyProductExists(Integer productId) {
        try {
            ProductDto productDto = fetchProduct(productId);
            if (productDto == null) {
                throw new ResourceNotFoundException(ErrorCode.PRODUCT_NOT_FOUND, productId);
            }
            
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException(ErrorCode.PRODUCT_NOT_FOUND, productId);
        } catch (RestClientException e) {
            log.error("Error communicating with product service for product {}: {}", 
                    productId, e.getMessage());
            throw new ExternalServiceException(
                    "Error communicating with product service", e);
        }
    }

    private void verifyFavouriteNotExists(Integer userId, Integer productId) {
        boolean favouriteExists = this.favouriteRepository
                .existsByUserIdAndProductId(userId, productId);
        
        if (favouriteExists) {
            throw new DuplicateResourceException(
                    ErrorCode.FAVOURITE_ALREADY_EXISTS, userId, productId);
        }
    }

    private UserDto fetchUser(Integer userId) {
        try {
            String url = AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/" + userId;
            log.debug("Fetching user from: {}", url);
            
            return this.restTemplate.getForObject(url, UserDto.class);
            
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, userId);
        } catch (RestClientException e) {
            log.error("Error fetching user {}: {}", userId, e.getMessage());
            throw new ExternalServiceException(
                    "Failed to communicate with user service", e);
        }
    }

    private ProductDto fetchProduct(Integer productId) {
        try {
            String url = AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/" + productId;
            log.debug("Fetching product from: {}", url);
            
            return this.restTemplate.getForObject(url, ProductDto.class);
            
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException(ErrorCode.PRODUCT_NOT_FOUND, productId);
        } catch (RestClientException e) {
            log.error("Error fetching product {}: {}", productId, e.getMessage());
            throw new ExternalServiceException(
                    "Failed to communicate with product service", e);
        }
    }
}