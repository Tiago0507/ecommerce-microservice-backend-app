package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.Cart;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.ErrorCode;
import com.selimhorri.app.exception.custom.ExternalServiceException;
import com.selimhorri.app.exception.custom.InvalidInputException;
import com.selimhorri.app.exception.custom.ResourceNotFoundException;
import com.selimhorri.app.helper.CartMappingHelper;
import com.selimhorri.app.repository.CartRepository;
import com.selimhorri.app.service.CartService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    @LoadBalanced
    private final RestTemplate restTemplate;

    @Override
    public List<CartDto> findAll() {
        log.info("Fetching all active carts");
        return this.cartRepository.findAllByIsActiveTrue()
                .stream()
                .map(CartMappingHelper::map)
                .map(c -> {
                    try {
                        c.setUserDto(this.restTemplate.getForObject(
                                AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/"
                                        + c.getUserDto().getUserId(),
                                UserDto.class));
                        return c;
                    } catch (HttpClientErrorException.NotFound e) {
                        log.warn("User not found for userId: {}", c.getUserDto().getUserId());
                        return c;
                    } catch (RestClientException e) {
                        log.error("Error fetching user data for userId: {}", c.getUserDto().getUserId(), e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public CartDto findById(final Integer cartId) {
        log.info("Fetching active cart with id: {}", cartId);
        
        Cart cart = this.cartRepository.findByCartIdAndIsActiveTrue(cartId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CART_NOT_FOUND, cartId));
        
        CartDto cartDto = CartMappingHelper.map(cart);
        
        try {
            UserDto userDto = this.restTemplate.getForObject(
                    AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/" + cartDto.getUserId(),
                    UserDto.class);
            cartDto.setUserDto(userDto);
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("User service returned 404 for userId: {}", cartDto.getUserId());
        } catch (RestClientException e) {
            throw new ExternalServiceException("Failed to retrieve user data from user-service", e);
        }
        
        return cartDto;
    }

    @Override
    public CartDto save(final CartDto cartDto) {
        log.info("Saving new cart");

        if (cartDto.getUserId() == null) {
            throw new InvalidInputException(ErrorCode.MISSING_REQUIRED_FIELD, "UserId is required");
        }

        try {
            final String url = AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/" + cartDto.getUserId();
            UserDto userDto = this.restTemplate.getForObject(url, UserDto.class);

            if (userDto == null) {
                throw new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, cartDto.getUserId());
            }

            cartDto.setUserDto(userDto);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, cartDto.getUserId());
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to verify user existence in user-service", ex);
        }

        cartDto.setCartId(null);
        cartDto.setOrderDtos(null);
        return CartMappingHelper.map(this.cartRepository.save(CartMappingHelper.map(cartDto)));
    }

    @Override
    public void deleteById(final Integer cartId) {
        log.info("Soft deleting cart with id: {}", cartId);

        Cart cart = this.cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CART_NOT_FOUND, cartId));

        cart.setActive(false);
        this.cartRepository.save(cart);

        log.debug("Cart with id: {} successfully deactivated", cartId);
    }
}