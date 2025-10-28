package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import org.springframework.stereotype.Service;

import com.selimhorri.app.domain.Credential;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.exception.ErrorCode;
import com.selimhorri.app.exception.custom.ResourceNotFoundException;
import com.selimhorri.app.helper.CredentialMappingHelper;
import com.selimhorri.app.repository.CredentialRepository;
import com.selimhorri.app.service.CredentialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class CredentialServiceImpl implements CredentialService {
    
    private final CredentialRepository credentialRepository;
    
    @Override
    public List<CredentialDto> findAll() {
        log.info("Fetching all credentials");
        return this.credentialRepository.findAll()
                .stream()
                .map(CredentialMappingHelper::map)
                .distinct()
                .collect(Collectors.toUnmodifiableList());
    }
    
    @Override
    public CredentialDto findById(final Integer credentialId) {
        log.info("Fetching credential with id: {}", credentialId);
        return this.credentialRepository.findById(credentialId)
                .map(CredentialMappingHelper::map)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CREDENTIAL_NOT_FOUND, credentialId));
    }
    
    @Override
    public CredentialDto save(final CredentialDto credentialDto) {
        log.info("Saving new credential");
        return CredentialMappingHelper.map(this.credentialRepository.save(CredentialMappingHelper.map(credentialDto)));
    }
    
    @Override
    public CredentialDto update(final CredentialDto credentialDto) {
        log.info("Updating credential with id: {}", credentialDto.getCredentialId());
        if (credentialDto.getCredentialId() != null && !this.credentialRepository.existsById(credentialDto.getCredentialId())) {
            throw new ResourceNotFoundException(ErrorCode.CREDENTIAL_NOT_FOUND, credentialDto.getCredentialId());
        }
    
        Credential existing = this.credentialRepository.findById(credentialDto.getCredentialId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CREDENTIAL_NOT_FOUND, credentialDto.getCredentialId()));
        
        existing.setUsername(credentialDto.getUsername());
        existing.setPassword(credentialDto.getPassword());
        existing.setRoleBasedAuthority(credentialDto.getRoleBasedAuthority());
        existing.setIsEnabled(credentialDto.getIsEnabled());
        existing.setIsAccountNonExpired(credentialDto.getIsAccountNonExpired());
        existing.setIsAccountNonLocked(credentialDto.getIsAccountNonLocked());
        existing.setIsCredentialsNonExpired(credentialDto.getIsCredentialsNonExpired());
        
        return CredentialMappingHelper.map(this.credentialRepository.save(existing));
    }
    
    @Override
    public CredentialDto update(final Integer credentialId, final CredentialDto credentialDto) {
        log.info("Updating credential with id: {}", credentialId);
        this.findById(credentialId);
        credentialDto.setCredentialId(credentialId);
        return this.update(credentialDto);
    }
    
    @Override
    public void deleteById(final Integer credentialId) {
        log.info("Deleting credential with id: {}", credentialId);
        if (!this.credentialRepository.existsById(credentialId)) {
            throw new ResourceNotFoundException(ErrorCode.CREDENTIAL_NOT_FOUND, credentialId);
        }
        this.credentialRepository.deleteById(credentialId);
    }
    
    @Override
    public CredentialDto findByUsername(final String username) {
        log.info("Fetching credential with username: {}", username);
        return this.credentialRepository.findByUsername(username)
                .map(CredentialMappingHelper::map)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USERNAME_NOT_FOUND, username));
    }
}