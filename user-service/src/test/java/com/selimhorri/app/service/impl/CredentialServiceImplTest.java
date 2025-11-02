package com.selimhorri.app.service.impl;

import com.selimhorri.app.domain.Credential;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.exception.ErrorCode;
import com.selimhorri.app.exception.custom.ResourceNotFoundException;
import com.selimhorri.app.repository.CredentialRepository;
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
class CredentialServiceImplTest {

    @Mock
    private CredentialRepository credentialRepository;

    @InjectMocks
    private CredentialServiceImpl credentialService;

    private Credential sample;

    @BeforeEach
    void setUp() {
        sample = Credential.builder()
                .credentialId(7)
                .username("jdoe")
                .password("secret")
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build();
    }

    @Test
    void findById_WhenExists_ReturnsDto() {
        // Arrange
        when(credentialRepository.findById(7)).thenReturn(Optional.of(sample));

        // Act
        CredentialDto result = credentialService.findById(7);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCredentialId()).isEqualTo(7);
        assertThat(result.getUsername()).isEqualTo("jdoe");
    }

    @Test
    void findById_WhenMissing_ThrowsResourceNotFoundException() {
        // Arrange
        when(credentialRepository.findById(77)).thenReturn(Optional.empty());

        // Act + Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> credentialService.findById(77));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CREDENTIAL_NOT_FOUND);
    }

    @Test
    void findByUsername_WhenExists_ReturnsDto() {
        // Arrange
        when(credentialRepository.findByUsername("jdoe")).thenReturn(Optional.of(sample));

        // Act
        CredentialDto result = credentialService.findByUsername("jdoe");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("jdoe");
    }

    @Test
    void findByUsername_WhenMissing_ThrowsResourceNotFoundException() {
        // Arrange
        when(credentialRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        // Act + Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> credentialService.findByUsername("ghost"));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USERNAME_NOT_FOUND);
    }

    @Test
    void update_WhenIdDoesNotExist_ThrowsResourceNotFoundException() {
        // Arrange
        CredentialDto update = CredentialDto.builder().credentialId(5).username("new").build();
        when(credentialRepository.existsById(5)).thenReturn(false);

        // Act + Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> credentialService.update(update));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CREDENTIAL_NOT_FOUND);
    }

    @Test
    void update_WhenValid_UpdatesFieldsAndSaves() {
        // Arrange
        Credential existing = Credential.builder()
                .credentialId(9)
                .username("old")
                .password("oldpass")
                .isEnabled(false)
                .isAccountNonExpired(false)
                .isAccountNonLocked(false)
                .isCredentialsNonExpired(false)
                .build();

        CredentialDto update = CredentialDto.builder()
                .credentialId(9)
                .username("new")
                .password("newpass")
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build();

        when(credentialRepository.existsById(9)).thenReturn(true);
        when(credentialRepository.findById(9)).thenReturn(Optional.of(existing));
        when(credentialRepository.save(any(Credential.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        CredentialDto result = credentialService.update(update);

        // Assert
        ArgumentCaptor<Credential> captor = ArgumentCaptor.forClass(Credential.class);
        verify(credentialRepository).save(captor.capture());
        Credential saved = captor.getValue();

        assertThat(saved.getUsername()).isEqualTo("new");
        assertThat(saved.getIsEnabled()).isTrue();
        assertThat(result.getPassword()).isEqualTo("newpass");
    }
}
