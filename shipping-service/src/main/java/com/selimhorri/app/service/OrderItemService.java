package com.selimhorri.app.service;

import java.util.List;

import com.selimhorri.app.dto.OrderItemDto;

public interface OrderItemService {
	
	List<OrderItemDto> findAll();
	OrderItemDto findById(final int shippingId);
	OrderItemDto save(final OrderItemDto orderItemDto);
	void deleteById(final int shippingId);

	List<OrderItemDto> findAllByOrderId(final int orderId);
	OrderItemDto findByOrderIdAndProductId(final int orderId, final int productId);
	void deleteByOrderIdAndProductId(final int orderId, final int productId);
	
}