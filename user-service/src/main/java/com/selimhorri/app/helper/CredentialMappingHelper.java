package com.selimhorri.app.helper;

import com.selimhorri.app.domain.Credential;
import com.selimhorri.app.domain.User;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.dto.UserDto;

public interface CredentialMappingHelper {
    
    static CredentialDto map(final Credential credential) {
        if (credential == null) return null;
        
        CredentialDto.CredentialDtoBuilder builder = CredentialDto.builder()
                .credentialId(credential.getCredentialId())
                .username(credential.getUsername())
                .password(credential.getPassword())
                .roleBasedAuthority(credential.getRoleBasedAuthority())
                .isEnabled(credential.getIsEnabled())
                .isAccountNonExpired(credential.getIsAccountNonExpired())
                .isAccountNonLocked(credential.getIsAccountNonLocked())
                .isCredentialsNonExpired(credential.getIsCredentialsNonExpired());
        
        if (credential.getUser() != null) {
            User user = credential.getUser();
            builder.userDto(UserDto.builder()
                    .userId(user.getUserId())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .imageUrl(user.getImageUrl())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .build());
        }
        
        return builder.build();
    }
    
    static Credential map(final CredentialDto credentialDto) {
        if (credentialDto == null) return null;
        
        Credential credential = Credential.builder()
                .credentialId(credentialDto.getCredentialId())
                .username(credentialDto.getUsername())
                .password(credentialDto.getPassword())
                .roleBasedAuthority(credentialDto.getRoleBasedAuthority())
                .isEnabled(credentialDto.getIsEnabled())
                .isAccountNonExpired(credentialDto.getIsAccountNonExpired())
                .isAccountNonLocked(credentialDto.getIsAccountNonLocked())
                .isCredentialsNonExpired(credentialDto.getIsCredentialsNonExpired())
                .build();
        
        if (credentialDto.getUserDto() != null) {
            UserDto userDto = credentialDto.getUserDto();
            User user = User.builder()
                    .userId(userDto.getUserId())
                    .firstName(userDto.getFirstName())
                    .lastName(userDto.getLastName())
                    .imageUrl(userDto.getImageUrl())
                    .email(userDto.getEmail())
                    .phone(userDto.getPhone())
                    .credential(credential)
                    .build();
            
            credential.setUser(user);
        }
        
        return credential;
    }
}