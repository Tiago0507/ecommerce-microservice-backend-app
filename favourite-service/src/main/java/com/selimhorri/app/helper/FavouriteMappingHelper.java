package com.selimhorri.app.helper;

import com.selimhorri.app.domain.Favourite;
import com.selimhorri.app.domain.id.FavouriteId;
import com.selimhorri.app.dto.FavouriteDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.UserDto;

public interface FavouriteMappingHelper {
	
	public static FavouriteDto map(final Favourite favourite) {
	if (favourite == null) return null;
	return FavouriteDto.builder()
		.userId(favourite.getUserId())
		.productId(favourite.getProductId())
		.likeDate(favourite.getLikeDate())
		.userDto(
			UserDto.builder()
			    .userId(favourite.getUserId())
			    .build())
		.productDto(
			ProductDto.builder()
			.productId(favourite.getProductId())
			.build())
		.build();
	}
	
	public static Favourite map(final FavouriteDto favouriteDto) {
		if (favouriteDto == null) return null;
		return Favourite.builder()
				.userId(favouriteDto.getUserId())
				.productId(favouriteDto.getProductId())
				.likeDate(favouriteDto.getLikeDate())
				.build();
	}

	public static FavouriteId toId(final Favourite favourite) {
		if (favourite == null) return null;
		return new FavouriteId(favourite.getUserId(), favourite.getProductId(), favourite.getLikeDate());
	}

	public static FavouriteId toId(final FavouriteDto favouriteDto) {
		if (favouriteDto == null) return null;
		return new FavouriteId(favouriteDto.getUserId(), favouriteDto.getProductId(), favouriteDto.getLikeDate());
	}
	
	
	
}









