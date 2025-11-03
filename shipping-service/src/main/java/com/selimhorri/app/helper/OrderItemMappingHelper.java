package com.selimhorri.app.helper;

import com.selimhorri.app.domain.OrderItem;
import com.selimhorri.app.domain.id.OrderItemId;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.OrderItemDto;
import com.selimhorri.app.dto.ProductDto;

public interface OrderItemMappingHelper {
    
	public static OrderItemDto map(final OrderItem orderItem) {
		if (orderItem == null) return null;

		OrderItemDto.OrderItemDtoBuilder builder = OrderItemDto.builder()
				.productId(orderItem.getProductId())
				.orderId(orderItem.getOrderId())
				.orderedQuantity(orderItem.getOrderedQuantity());

		if (orderItem.getProductId() != null) {
			builder.productDto(ProductDto.builder()
					.productId(orderItem.getProductId())
					.build());
		}

		if (orderItem.getOrderId() != null) {
			builder.orderDto(OrderDto.builder()
					.orderId(orderItem.getOrderId())
					.build());
		}

		return builder.build();
	}

	public static OrderItem map(final OrderItemDto orderItemDto) {
		if (orderItemDto == null) return null;

		return OrderItem.builder()
				.productId(orderItemDto.getProductId())
				.orderId(orderItemDto.getOrderId())
				.orderedQuantity(orderItemDto.getOrderedQuantity())
				.build();
	}

	/**
	 * Helper to get composite id from entity
	 */
	public static OrderItemId toId(final OrderItem entity) {
		if (entity == null) return null;
		return new OrderItemId(entity.getProductId(), entity.getOrderId());
	}

	/**
	 * Helper to get composite id from dto
	 */
	public static OrderItemId toId(final OrderItemDto dto) {
		if (dto == null) return null;
		return new OrderItemId(dto.getProductId(), dto.getOrderId());
	}
    

}









