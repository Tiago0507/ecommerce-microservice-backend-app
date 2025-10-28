package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import org.springframework.stereotype.Service;

import com.selimhorri.app.domain.VerificationToken;
import com.selimhorri.app.dto.VerificationTokenDto;
import com.selimhorri.app.exception.ErrorCode;
import com.selimhorri.app.exception.custom.ResourceNotFoundException;
import com.selimhorri.app.helper.VerificationTokenMappingHelper;
import com.selimhorri.app.repository.VerificationTokenRepository;
import com.selimhorri.app.service.VerificationTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class VerificationTokenServiceImpl implements VerificationTokenService {
    
    private final VerificationTokenRepository verificationTokenRepository;
    
    @Override
    public List<VerificationTokenDto> findAll() {
        log.info("Fetching all verification tokens");
        return this.verificationTokenRepository.findAll()
                .stream()
                .map(VerificationTokenMappingHelper::map)
                .distinct()
                .collect(Collectors.toUnmodifiableList());
    }
    
    @Override
    public VerificationTokenDto findById(final Integer verificationTokenId) {
        log.info("Fetching verification token with id: {}", verificationTokenId);
        return this.verificationTokenRepository.findById(verificationTokenId)
                .map(VerificationTokenMappingHelper::map)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VERIFICATION_TOKEN_NOT_FOUND, verificationTokenId));
    }
    
    @Override
    public VerificationTokenDto save(final VerificationTokenDto verificationTokenDto) {
        log.info("Saving new verification token");
        return VerificationTokenMappingHelper.map(this.verificationTokenRepository
                .save(VerificationTokenMappingHelper.map(verificationTokenDto)));
    }
    
    @Override
    public VerificationTokenDto update(final VerificationTokenDto verificationTokenDto) {
        log.info("Updating verification token with id: {}", verificationTokenDto.getVerificationTokenId());
        if (verificationTokenDto.getVerificationTokenId() != null && 
            !this.verificationTokenRepository.existsById(verificationTokenDto.getVerificationTokenId())) {
            throw new ResourceNotFoundException(ErrorCode.VERIFICATION_TOKEN_NOT_FOUND, verificationTokenDto.getVerificationTokenId());
        }
        
        VerificationToken existing = this.verificationTokenRepository.findById(verificationTokenDto.getVerificationTokenId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VERIFICATION_TOKEN_NOT_FOUND, verificationTokenDto.getVerificationTokenId()));
        
        existing.setToken(verificationTokenDto.getToken());
        existing.setExpireDate(verificationTokenDto.getExpireDate());
        
        return VerificationTokenMappingHelper.map(this.verificationTokenRepository.save(existing));
    }
    
    @Override
    public VerificationTokenDto update(final Integer verificationTokenId, final VerificationTokenDto verificationTokenDto) {
        log.info("Updating verification token with id: {}", verificationTokenId);
        this.findById(verificationTokenId);
        verificationTokenDto.setVerificationTokenId(verificationTokenId);
        return this.update(verificationTokenDto);
    }
    
    @Override
    public void deleteById(final Integer verificationTokenId) {
        log.info("Deleting verification token with id: {}", verificationTokenId);
        if (!this.verificationTokenRepository.existsById(verificationTokenId)) {
            throw new ResourceNotFoundException(ErrorCode.VERIFICATION_TOKEN_NOT_FOUND, verificationTokenId);
        }
        this.verificationTokenRepository.deleteById(verificationTokenId);
    }
}