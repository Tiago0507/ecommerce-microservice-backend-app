package com.selimhorri.app.helper;

import com.selimhorri.app.domain.Address;
import com.selimhorri.app.domain.User;
import com.selimhorri.app.dto.AddressDto;
import com.selimhorri.app.dto.UserDto;

public interface AddressMappingHelper {
    
    public static AddressDto map(final Address address) {
        if (address == null) return null;
        
        return AddressDto.builder()
                .addressId(address.getAddressId())
                .fullAddress(address.getFullAddress())
                .postalCode(address.getPostalCode())
                .city(address.getCity())
                .userDto(address.getUser() != null ? 
                    UserDto.builder()
                        .userId(address.getUser().getUserId())
                        .firstName(address.getUser().getFirstName())
                        .lastName(address.getUser().getLastName())
                        .imageUrl(address.getUser().getImageUrl())
                        .email(address.getUser().getEmail())
                        .phone(address.getUser().getPhone())
                        .build() : null)
                .build();
    }
    
    public static Address map(final AddressDto addressDto) {
        if (addressDto == null) return null;
        
        Address address = Address.builder()
                .addressId(addressDto.getAddressId())
                .fullAddress(addressDto.getFullAddress())
                .postalCode(addressDto.getPostalCode())
                .city(addressDto.getCity())
                .build();
        
        if (addressDto.getUserDto() != null) {
            UserDto userDto = addressDto.getUserDto();
            User user = User.builder()
                    .userId(userDto.getUserId())
                    .firstName(userDto.getFirstName())
                    .lastName(userDto.getLastName())
                    .imageUrl(userDto.getImageUrl())
                    .email(userDto.getEmail())
                    .phone(userDto.getPhone())
                    .build();
            
            address.setUser(user);
        }
        
        return address;
    }
}