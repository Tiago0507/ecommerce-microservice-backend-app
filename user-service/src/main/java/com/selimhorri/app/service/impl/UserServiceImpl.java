package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.selimhorri.app.domain.Credential;
import com.selimhorri.app.domain.User;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.ErrorCode;
import com.selimhorri.app.exception.custom.DuplicateResourceException;
import com.selimhorri.app.exception.custom.ResourceNotFoundException;
import com.selimhorri.app.helper.UserMappingHelper;
import com.selimhorri.app.repository.UserRepository;
import com.selimhorri.app.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    
    @Override
    public List<UserDto> findAll() {
        log.info("Fetching all users");
        return this.userRepository.findAllWithCredentials()
                .stream()
                .map(UserMappingHelper::map)
                .distinct()
                .collect(Collectors.toUnmodifiableList());
    }
    
    @Override
    public UserDto findById(final Integer userId) {
    log.info("Fetching user with id: {}", userId);
    return this.userRepository.findByIdWithCredential(userId)
            .map(UserMappingHelper::map)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, userId));
}
    
    @Override
    public UserDto save(final UserDto userDto) {
        log.info("Saving new user");
        
        if (userDto.getEmail() != null && this.userRepository.existsByEmailIgnoreCase(userDto.getEmail())) {
            throw new DuplicateResourceException(ErrorCode.EMAIL_ALREADY_REGISTERED, userDto.getEmail());
        }
        
        if (userDto.getCredentialDto() != null && userDto.getCredentialDto().getUsername() != null) {
            String username = userDto.getCredentialDto().getUsername();
            if (this.userRepository.findByCredentialUsername(username).isPresent()) {
                throw new DuplicateResourceException(ErrorCode.USERNAME_ALREADY_TAKEN, username);
            }
        }
        
        try {
            return UserMappingHelper.map(this.userRepository.save(UserMappingHelper.map(userDto)));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateResourceException(ErrorCode.USER_ALREADY_EXISTS);
        }
    }
    
    @Override
    public UserDto update(final UserDto userDto) {
        log.info("Updating user with id: {}", userDto.getUserId());
        if (userDto.getUserId() != null && !this.userRepository.existsById(userDto.getUserId())) {
            throw new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, userDto.getUserId());
        }
    
        User existingUser = this.userRepository.findById(userDto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, userDto.getUserId()));
    
        existingUser.setFirstName(userDto.getFirstName());
        existingUser.setLastName(userDto.getLastName());
        existingUser.setImageUrl(userDto.getImageUrl());
        existingUser.setEmail(userDto.getEmail());
        existingUser.setPhone(userDto.getPhone());
    
    
        if (userDto.getCredentialDto() != null && existingUser.getCredential() != null) {
            Credential existingCredential = existingUser.getCredential();
            CredentialDto credDto = userDto.getCredentialDto();
            
            existingCredential.setUsername(credDto.getUsername());
            existingCredential.setPassword(credDto.getPassword());
            existingCredential.setRoleBasedAuthority(credDto.getRoleBasedAuthority());
            existingCredential.setIsEnabled(credDto.getIsEnabled());
            existingCredential.setIsAccountNonExpired(credDto.getIsAccountNonExpired());
            existingCredential.setIsAccountNonLocked(credDto.getIsAccountNonLocked());
            existingCredential.setIsCredentialsNonExpired(credDto.getIsCredentialsNonExpired());
        }
    
        return UserMappingHelper.map(this.userRepository.save(existingUser));
    }
    
    @Override
    public UserDto update(final Integer userId, final UserDto userDto) {
        log.info("Updating user with id: {}", userId);
        this.findById(userId);
        userDto.setUserId(userId);
        return this.update(userDto);
    }
    
    @Override
    public void deleteById(final Integer userId) {
        log.info("Deleting user with id: {}", userId);
        if (!this.userRepository.existsById(userId)) {
            throw new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, userId);
        }
        this.userRepository.deleteById(userId);
    }
    
    @Override
    public UserDto findByUsername(final String username) {
        log.info("Fetching user with username: {}", username);
        return this.userRepository.findByCredentialUsername(username)
                .map(UserMappingHelper::map)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USERNAME_NOT_FOUND, username));
    }
}