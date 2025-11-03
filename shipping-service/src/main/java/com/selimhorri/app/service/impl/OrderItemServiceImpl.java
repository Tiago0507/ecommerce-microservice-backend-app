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
import com.selimhorri.app.domain.OrderItem;
import com.selimhorri.app.domain.id.OrderItemId;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.OrderItemDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.exception.ErrorCode;
import com.selimhorri.app.exception.custom.DuplicateResourceException;
import com.selimhorri.app.exception.custom.ExternalServiceException;
import com.selimhorri.app.exception.custom.InvalidInputException;
import com.selimhorri.app.exception.custom.ResourceNotFoundException;
import com.selimhorri.app.helper.OrderItemMappingHelper;
import com.selimhorri.app.repository.OrderItemRepository;
import com.selimhorri.app.service.OrderItemService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class OrderItemServiceImpl implements OrderItemService {
	
	private final OrderItemRepository orderItemRepository;
	private final RestTemplate restTemplate;
	
	@Override
	public List<OrderItemDto> findAll() {
		log.info("*** OrderItemDto List, service; fetch all orderItems *");
		return this.orderItemRepository.findAll()
				.stream()
				.map(OrderItemMappingHelper::map)
				.map(this::enrichOrderItemWithExternalData)
				.filter(Objects::nonNull)
				.distinct()
				.collect(Collectors.toUnmodifiableList());
	}
	
	@Override
	public OrderItemDto findById(final OrderItemId orderItemId) {
		log.info("*** OrderItemDto, service; fetch orderItem by id *");
		return this.orderItemRepository.findById(orderItemId)
				.map(OrderItemMappingHelper::map)
				.map(this::enrichOrderItemWithExternalData)
				.orElseThrow(() -> new ResourceNotFoundException(
						ErrorCode.SHIPPING_NOT_FOUND, orderItemId));
	}
	
	@Override
	public OrderItemDto save(final OrderItemDto orderItemDto) {
		log.info("*** OrderItemDto, service; save orderItem *");
		
		if (orderItemDto == null || orderItemDto.getOrderId() == null 
				|| orderItemDto.getProductId() == null) {
			throw new InvalidInputException(ErrorCode.MISSING_REQUIRED_FIELD);
		}

		final OrderItemId id = OrderItemMappingHelper.toId(orderItemDto);
		if (this.orderItemRepository.existsById(id)) {
			throw new DuplicateResourceException(ErrorCode.DUPLICATE_RESOURCE, id);
		}
		
		// Verificar que el producto existe antes de guardar
		verifyProductExists(orderItemDto.getProductId());
		
		// Verificar que la orden existe antes de guardar
		verifyOrderExists(orderItemDto.getOrderId());

		try {
			OrderItem saved = this.orderItemRepository.save(
					OrderItemMappingHelper.map(orderItemDto));
			return OrderItemMappingHelper.map(saved);
		} catch (org.springframework.dao.DataIntegrityViolationException e) {
			throw new DuplicateResourceException(ErrorCode.DUPLICATE_RESOURCE);
		}
	}
	
	@Override
	public OrderItemDto update(final OrderItemDto orderItemDto) {
		log.info("*** OrderItemDto, service; update orderItem *");
		
		if (orderItemDto == null || orderItemDto.getOrderId() == null 
				|| orderItemDto.getProductId() == null) {
			throw new InvalidInputException(ErrorCode.MISSING_REQUIRED_FIELD);
		}

		final OrderItem entity = OrderItemMappingHelper.map(orderItemDto);
		final OrderItemId entityId = OrderItemMappingHelper.toId(entity);
		
		if (!this.orderItemRepository.existsById(entityId)) {
			throw new ResourceNotFoundException(ErrorCode.SHIPPING_NOT_FOUND, entityId);
		}

		return OrderItemMappingHelper.map(this.orderItemRepository.save(entity));
	}
	
	@Override
	public void deleteById(final OrderItemId orderItemId) {
		log.info("*** Void, service; delete orderItem by id *");
		
		if (!this.orderItemRepository.existsById(orderItemId)) {
			throw new ResourceNotFoundException(ErrorCode.SHIPPING_NOT_FOUND, orderItemId);
		}

		this.orderItemRepository.deleteById(orderItemId);
	}
	
	/**
	 * Enriquece el OrderItemDto con datos de servicios externos
	 */
	private OrderItemDto enrichOrderItemWithExternalData(OrderItemDto orderItemDto) {
		try {
			// Obtener producto
			ProductDto product = fetchProduct(orderItemDto.getProductDto().getProductId());
			orderItemDto.setProductDto(product);
			
			// Obtener orden
			OrderDto order = fetchOrder(orderItemDto.getOrderDto().getOrderId());
			orderItemDto.setOrderDto(order);
			
			return orderItemDto;
		} catch (HttpClientErrorException.NotFound e) {
			log.warn("Resource not found while enriching order item: {}", e.getMessage());
			// Retornar el DTO con datos parciales en lugar de fallar
			return orderItemDto;
		} catch (RestClientException e) {
			log.error("Error communicating with external service: {}", e.getMessage());
			// En findAll, podemos omitir items con errores
			return null;
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
	 * Obtiene una orden del servicio externo
	 */
	private OrderDto fetchOrder(Integer orderId) {
		try {
			String url = AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL 
					+ "/" + orderId;
			log.debug("Fetching order from: {}", url);
			return this.restTemplate.getForObject(url, OrderDto.class);
		} catch (HttpClientErrorException.NotFound e) {
			log.warn("Order {} not found in order-service", orderId);
			throw new ExternalServiceException(
					ErrorCode.ORDER_NOT_FOUND.formatMessage(orderId), e);
		} catch (RestClientException e) {
			log.error("Error fetching order {}: {}", orderId, e.getMessage());
			throw new ExternalServiceException(
					"Failed to communicate with order-service", e);
		}
	}
	
	/**
	 * Verifica que un producto existe antes de crear un OrderItem
	 */
	private void verifyProductExists(Integer productId) {
		try {
			fetchProduct(productId);
		} catch (ExternalServiceException e) {
			throw new InvalidInputException(
					ErrorCode.PRODUCT_NOT_FOUND, 
					"Cannot create order item: " + e.getMessage());
		}
	}
	
	/**
	 * Verifica que una orden existe antes de crear un OrderItem
	 */
	private void verifyOrderExists(Integer orderId) {
		try {
			fetchOrder(orderId);
		} catch (ExternalServiceException e) {
			throw new InvalidInputException(
					ErrorCode.ORDER_NOT_FOUND, 
					"Cannot create order item: " + e.getMessage());
		}
	}
}


