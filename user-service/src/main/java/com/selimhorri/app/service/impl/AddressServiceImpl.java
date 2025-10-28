package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import org.springframework.stereotype.Service;

import com.selimhorri.app.domain.Address;
import com.selimhorri.app.dto.AddressDto;
import com.selimhorri.app.exception.ErrorCode;
import com.selimhorri.app.exception.custom.ResourceNotFoundException;
import com.selimhorri.app.helper.AddressMappingHelper;
import com.selimhorri.app.repository.AddressRepository;
import com.selimhorri.app.service.AddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {
    
    private final AddressRepository addressRepository;
    
    @Override
    public List<AddressDto> findAll() {
        log.info("Fetching all addresses");
        return this.addressRepository.findAll()
                .stream()
                .map(AddressMappingHelper::map)
                .distinct()
                .collect(Collectors.toUnmodifiableList());
    }
    
    @Override
    public AddressDto findById(final Integer addressId) {
        log.info("Fetching address with id: {}", addressId);
        return this.addressRepository.findById(addressId)
                .map(AddressMappingHelper::map)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ADDRESS_NOT_FOUND, addressId));
    }
    
    @Override
    public AddressDto save(final AddressDto addressDto) {
        log.info("Saving new address");
        return AddressMappingHelper.map(this.addressRepository.save(AddressMappingHelper.map(addressDto)));
    }
    
    @Override
    public AddressDto update(final AddressDto addressDto) {
        log.info("Updating address with id: {}", addressDto.getAddressId());
        if (addressDto.getAddressId() != null && !this.addressRepository.existsById(addressDto.getAddressId())) {
            throw new ResourceNotFoundException(ErrorCode.ADDRESS_NOT_FOUND, addressDto.getAddressId());
        }
        
        Address existing = this.addressRepository.findById(addressDto.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ADDRESS_NOT_FOUND, addressDto.getAddressId()));
        
        existing.setFullAddress(addressDto.getFullAddress());
        existing.setPostalCode(addressDto.getPostalCode());
        existing.setCity(addressDto.getCity());
        
        return AddressMappingHelper.map(this.addressRepository.save(existing));
    }
    
    @Override
    public AddressDto update(final Integer addressId, final AddressDto addressDto) {
        log.info("Updating address with id: {}", addressId);
        this.findById(addressId);
        addressDto.setAddressId(addressId);
        return this.update(addressDto);
    }
    
    @Override
    public void deleteById(final Integer addressId) {
        log.info("Deleting address with id: {}", addressId);
        if (!this.addressRepository.existsById(addressId)) {
            throw new ResourceNotFoundException(ErrorCode.ADDRESS_NOT_FOUND, addressId);
        }
        this.addressRepository.deleteById(addressId);
    }
}