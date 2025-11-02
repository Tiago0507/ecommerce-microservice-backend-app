package com.selimhorri.app.service.impl;

import com.selimhorri.app.domain.Credential;
import com.selimhorri.app.domain.User;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.ErrorCode;
import com.selimhorri.app.exception.custom.DuplicateResourceException;
import com.selimhorri.app.exception.custom.ResourceNotFoundException;
import com.selimhorri.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User sampleUser;
    private Credential sampleCredential;

    @BeforeEach
    void setUp() {
        sampleCredential = Credential.builder()
                .credentialId(10)
                .username("jdoe")
                .password("secret")
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build();

        sampleUser = User.builder()
                .userId(1)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("123456789")
                .credential(sampleCredential)
                .build();

        // back-reference for bidirectional
        sampleCredential.setUser(sampleUser);
    }

    // save
    @Test
    void save_WhenEmailAlreadyRegistered_ThrowsDuplicateResourceException() {
        // Arrange
        UserDto input = UserDto.builder()
                .email("john.doe@example.com")
                .credentialDto(CredentialDto.builder().username("jdoe").build())
                .build();
        when(userRepository.existsByEmailIgnoreCase("john.doe@example.com")).thenReturn(true);

        // Act + Assert
        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> userService.save(input));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_REGISTERED);
        verify(userRepository, never()).save(any());
    }

    @Test
    void save_WhenUsernameAlreadyTaken_ThrowsDuplicateResourceException() {
        // Arrange
        UserDto input = UserDto.builder()
                .email("new.email@example.com")
                .credentialDto(CredentialDto.builder().username("jdoe").build())
                .build();
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.findByCredentialUsername("jdoe")).thenReturn(Optional.of(sampleUser));

        // Act + Assert
        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> userService.save(input));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USERNAME_ALREADY_TAKEN);
        verify(userRepository, never()).save(any());
    }

    @Test
    void save_WhenDataIntegrityViolation_ThrowsDuplicateResourceException() {
        // Arrange
        UserDto input = UserDto.builder()
                .email("unique@example.com")
                .credentialDto(CredentialDto.builder().username("uniqueuser").build())
                .build();
        when(userRepository.existsByEmailIgnoreCase("unique@example.com")).thenReturn(false);
        when(userRepository.findByCredentialUsername("uniqueuser")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenThrow(new DataIntegrityViolationException("constraint"));

        // Act + Assert
        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> userService.save(input));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_ALREADY_EXISTS);
    }

    // findById
    @Test
    void findById_WhenUserExists_ReturnsMappedUserDto() {
        // Arrange
        when(userRepository.findByIdWithCredential(1)).thenReturn(Optional.of(sampleUser));

        // Act
        UserDto result = userService.findById(1);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(1);
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getCredentialDto()).isNotNull();
        assertThat(result.getCredentialDto().getUsername()).isEqualTo("jdoe");
    }

    @Test
    void findById_WhenUserMissing_ThrowsResourceNotFoundException() {
        // Arrange
        when(userRepository.findByIdWithCredential(99)).thenReturn(Optional.empty());

        // Act + Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> userService.findById(99));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    // update
    @Test
    void update_WhenUserIdDoesNotExist_ThrowsResourceNotFoundException() {
        // Arrange
        UserDto update = UserDto.builder().userId(42).firstName("Jane").build();
        when(userRepository.existsById(42)).thenReturn(false);

        // Act + Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> userService.update(update));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void update_WhenCredentialPresent_UpdatesNestedCredentialAndSaves() {
        // Arrange
        User existing = User.builder()
                .userId(5)
                .firstName("Old")
                .lastName("Name")
                .email("old@example.com")
                .credential(Credential.builder()
                        .credentialId(50)
                        .username("olduser")
                        .password("oldpass")
                        .isEnabled(false)
                        .isAccountNonExpired(false)
                        .isAccountNonLocked(false)
                        .isCredentialsNonExpired(false)
                        .build())
                .build();
        existing.getCredential().setUser(existing);

        UserDto update = UserDto.builder()
                .userId(5)
                .firstName("New")
                .lastName("Name")
                .email("new@example.com")
                .credentialDto(CredentialDto.builder()
                        .credentialId(50)
                        .username("newuser")
                        .password("newpass")
                        .isEnabled(true)
                        .isAccountNonExpired(true)
                        .isAccountNonLocked(true)
                        .isCredentialsNonExpired(true)
                        .build())
                .build();

        when(userRepository.existsById(5)).thenReturn(true);
        when(userRepository.findById(5)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        UserDto result = userService.update(update);

        // Assert
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getFirstName()).isEqualTo("New");
        assertThat(saved.getEmail()).isEqualTo("new@example.com");
        assertThat(saved.getCredential()).isNotNull();
        assertThat(saved.getCredential().getUsername()).isEqualTo("newuser");
        assertThat(saved.getCredential().getIsEnabled()).isTrue();

        assertThat(result.getCredentialDto()).isNotNull();
        assertThat(result.getCredentialDto().getUsername()).isEqualTo("newuser");
    }
}
