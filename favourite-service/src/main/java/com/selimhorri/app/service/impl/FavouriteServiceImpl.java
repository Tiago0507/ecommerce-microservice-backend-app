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
import com.selimhorri.app.domain.Favourite;
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
		log.info("*** FavouriteDto List, service; fetch all favourites *");
		return this.favouriteRepository.findAll()
				.stream()
				.map(FavouriteMappingHelper::map)
				.map(this::enrichFavouriteWithExternalData)
				.filter(Objects::nonNull)
				.distinct()
				.collect(Collectors.toUnmodifiableList());
	}
	
	@Override
	public FavouriteDto findById(final FavouriteId favouriteId) {
		log.info("*** FavouriteDto, service; fetch favourite by id *");
		return this.favouriteRepository.findById(favouriteId)
				.map(FavouriteMappingHelper::map)
				.map(this::enrichFavouriteWithExternalData)
				.orElseThrow(() -> new ResourceNotFoundException(
						ErrorCode.FAVOURITE_NOT_FOUND, favouriteId));
	}
	
	@Override
	public FavouriteDto save(final FavouriteDto favouriteDto) {
		log.info("*** FavouriteDto, service; save favourite *");
		
		if (favouriteDto == null || favouriteDto.getUserId() == null 
				|| favouriteDto.getProductId() == null) {
			throw new InvalidInputException(ErrorCode.MISSING_REQUIRED_FIELD);
		}
		
		final FavouriteId id = FavouriteMappingHelper.toId(favouriteDto);
		if (this.favouriteRepository.existsById(id)) {
			throw new DuplicateResourceException(ErrorCode.DUPLICATE_RESOURCE, id);
		}
		
		// Verificar que el usuario existe
		verifyUserExists(favouriteDto.getUserId());
		
		// Verificar que el producto existe
		verifyProductExists(favouriteDto.getProductId());
		
		try {
			Favourite saved = this.favouriteRepository.save(
					FavouriteMappingHelper.map(favouriteDto));
			return FavouriteMappingHelper.map(saved);
		} catch (org.springframework.dao.DataIntegrityViolationException e) {
			throw new DuplicateResourceException(ErrorCode.DUPLICATE_RESOURCE);
		}
	}
	
	@Override
	public FavouriteDto update(final FavouriteDto favouriteDto) {
		log.info("*** FavouriteDto, service; update favourite *");
		
		if (favouriteDto == null || favouriteDto.getUserId() == null 
				|| favouriteDto.getProductId() == null) {
			throw new InvalidInputException(ErrorCode.MISSING_REQUIRED_FIELD);
		}
		
		final Favourite entity = FavouriteMappingHelper.map(favouriteDto);
		final FavouriteId entityId = FavouriteMappingHelper.toId(entity);
		
		if (!this.favouriteRepository.existsById(entityId)) {
			throw new ResourceNotFoundException(ErrorCode.FAVOURITE_NOT_FOUND, entityId);
		}
		
		return FavouriteMappingHelper.map(this.favouriteRepository.save(entity));
	}
	
	@Override
	public void deleteById(final FavouriteId favouriteId) {
		log.info("*** Void, service; delete favourite by id *");
		
		if (!this.favouriteRepository.existsById(favouriteId)) {
			throw new ResourceNotFoundException(ErrorCode.FAVOURITE_NOT_FOUND, favouriteId);
		}
		
		this.favouriteRepository.deleteById(favouriteId);
	}
	
	/**
	 * Enriquece el FavouriteDto con datos de servicios externos
	 */
	private FavouriteDto enrichFavouriteWithExternalData(FavouriteDto favouriteDto) {
		try {
			// Obtener usuario
			UserDto user = fetchUser(favouriteDto.getUserId());
			favouriteDto.setUserDto(user);
			
			// Obtener producto
			ProductDto product = fetchProduct(favouriteDto.getProductId());
			favouriteDto.setProductDto(product);
			
			return favouriteDto;
		} catch (HttpClientErrorException.NotFound e) {
			log.warn("Resource not found while enriching favourite: {}", e.getMessage());
			// Retornar el DTO con datos parciales en lugar de fallar
			return favouriteDto;
		} catch (RestClientException e) {
			log.error("Error communicating with external service: {}", e.getMessage());
			// En findAll, podemos omitir items con errores
			return null;
		}
	}
	
	/**
	 * Obtiene un usuario del servicio externo
	 */
	private UserDto fetchUser(Integer userId) {
		try {
			String url = AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL 
					+ "/" + userId;
			log.debug("Fetching user from: {}", url);
			return this.restTemplate.getForObject(url, UserDto.class);
		} catch (HttpClientErrorException.NotFound e) {
			log.warn("User {} not found in user-service", userId);
			throw new ExternalServiceException(
					ErrorCode.USER_NOT_FOUND.formatMessage(userId), e);
		} catch (RestClientException e) {
			log.error("Error fetching user {}: {}", userId, e.getMessage());
			throw new ExternalServiceException(
					"Failed to communicate with user-service", e);
		}
	}
	
	/**
	 * Obtiene un producto del servicio externo
	 */
	private ProductDto fetchProduct(Integer productId) {
		try {
			String url = AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL 
					+ "/" + productId;
			log.debug("Fetching product from: {}", url);
			return this.restTemplate.getForObject(url, ProductDto.class);
		} catch (HttpClientErrorException.NotFound e) {
			log.warn("Product {} not found in product-service", productId);
			throw new ExternalServiceException(
					ErrorCode.PRODUCT_NOT_FOUND.formatMessage(productId), e);
		} catch (RestClientException e) {
			log.error("Error fetching product {}: {}", productId, e.getMessage());
			throw new ExternalServiceException(
					"Failed to communicate with product-service", e);
		}
	}
	
	/**
	 * Verifica que un usuario existe antes de crear un Favourite
	 */
	private void verifyUserExists(Integer userId) {
		try {
			fetchUser(userId);
		} catch (ExternalServiceException e) {
			throw new InvalidInputException(
					ErrorCode.USER_NOT_FOUND, 
					"Cannot create favourite: " + e.getMessage());
		}
	}
	
	/**
	 * Verifica que un producto existe antes de crear un Favourite
	 */
	private void verifyProductExists(Integer productId) {
		try {
			fetchProduct(productId);
		} catch (ExternalServiceException e) {
			throw new InvalidInputException(
					ErrorCode.PRODUCT_NOT_FOUND, 
					"Cannot create favourite: " + e.getMessage());
		}
	}
}




