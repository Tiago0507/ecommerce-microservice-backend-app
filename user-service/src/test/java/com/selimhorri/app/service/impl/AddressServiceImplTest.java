package com.selimhorri.app.service.impl;

import com.selimhorri.app.domain.Address;
import com.selimhorri.app.dto.AddressDto;
import com.selimhorri.app.exception.ErrorCode;
import com.selimhorri.app.exception.custom.ResourceNotFoundException;
import com.selimhorri.app.repository.AddressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {

    @Mock
    private AddressRepository addressRepository;

    @InjectMocks
    private AddressServiceImpl addressService;

    private Address sample;

    @BeforeEach
    void setUp() {
        sample = Address.builder()
                .addressId(1)
                .fullAddress("Street 1")
                .postalCode("11111")
                .city("Bogota")
                .build();
    }

    @Test
    void findById_WhenAddressExists_ReturnsDto() {
        // Arrange
        when(addressRepository.findById(1)).thenReturn(Optional.of(sample));

        // Act
        AddressDto result = addressService.findById(1);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAddressId()).isEqualTo(1);
        assertThat(result.getCity()).isEqualTo("Bogota");
    }

    @Test
    void findById_WhenAddressMissing_ThrowsResourceNotFoundException() {
        // Arrange
        when(addressRepository.findById(99)).thenReturn(Optional.empty());

        // Act + Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> addressService.findById(99));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ADDRESS_NOT_FOUND);
    }

    @Test
    void update_WhenIdDoesNotExist_ThrowsResourceNotFoundException() {
        // Arrange
        AddressDto update = AddressDto.builder().addressId(10).city("Medellin").build();
        when(addressRepository.existsById(10)).thenReturn(false);

        // Act + Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> addressService.update(update));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ADDRESS_NOT_FOUND);
    }

    @Test
    void update_WhenValid_UpdatesFieldsAndSaves() {
        // Arrange
        Address existing = Address.builder()
                .addressId(5)
                .fullAddress("Old St")
                .postalCode("00000")
                .city("Cali")
                .build();

        AddressDto update = AddressDto.builder()
                .addressId(5)
                .fullAddress("New St")
                .postalCode("99999")
                .city("Barranquilla")
                .build();

        when(addressRepository.existsById(5)).thenReturn(true);
        when(addressRepository.findById(5)).thenReturn(Optional.of(existing));
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        AddressDto result = addressService.update(update);

        // Assert
        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).save(captor.capture());
        Address saved = captor.getValue();

        assertThat(saved.getFullAddress()).isEqualTo("New St");
        assertThat(saved.getCity()).isEqualTo("Barranquilla");
        assertThat(result.getPostalCode()).isEqualTo("99999");
    }

    @Test
    void deleteById_WhenAddressMissing_ThrowsResourceNotFoundException() {
        // Arrange
        when(addressRepository.existsById(50)).thenReturn(false);

        // Act + Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> addressService.deleteById(50));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ADDRESS_NOT_FOUND);
        verify(addressRepository, never()).deleteById(anyInt());
    }
}
