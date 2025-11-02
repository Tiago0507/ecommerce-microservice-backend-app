package com.selimhorri.app.domain;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_items")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
public final class OrderItem extends AbstractMappedEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "order_id", nullable = false, updatable = false)
	private Integer orderId;

	@Column(name = "product_id", nullable = false, updatable = false)
	private Integer productId;

	@Column(name = "ordered_quantity")
	private Integer orderedQuantity;

	@Column(name = "is_active")
	private boolean isActive;

}