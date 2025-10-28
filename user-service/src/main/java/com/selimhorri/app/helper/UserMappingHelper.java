package com.selimhorri.app.helper;

import com.selimhorri.app.domain.Credential;
import com.selimhorri.app.domain.User;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.dto.UserDto;

public interface UserMappingHelper {
    
    static UserDto map(final User user) {
        if (user == null) return null;
        
        UserDto.UserDtoBuilder builder = UserDto.builder()
                .userId(user.getUserId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .imageUrl(user.getImageUrl())
                .email(user.getEmail())
                .phone(user.getPhone());
        
        if (user.getCredential() != null) {
            Credential credential = user.getCredential();
            builder.credentialDto(CredentialDto.builder()
                    .credentialId(credential.getCredentialId())
                    .username(credential.getUsername())
                    .password(credential.getPassword())
                    .roleBasedAuthority(credential.getRoleBasedAuthority())
                    .isEnabled(credential.getIsEnabled())
                    .isAccountNonExpired(credential.getIsAccountNonExpired())
                    .isAccountNonLocked(credential.getIsAccountNonLocked())
                    .isCredentialsNonExpired(credential.getIsCredentialsNonExpired())
                    .build());
        }
        
        return builder.build();
    }
    
    static User map(final UserDto userDto) {
        if (userDto == null) return null;
        
        User user = User.builder()
                .userId(userDto.getUserId())
                .firstName(userDto.getFirstName())
                .lastName(userDto.getLastName())
                .imageUrl(userDto.getImageUrl())
                .email(userDto.getEmail())
                .phone(userDto.getPhone())
                .build();
        
        if (userDto.getCredentialDto() != null) {
            CredentialDto credDto = userDto.getCredentialDto();
            Credential credential = Credential.builder()
                    .credentialId(credDto.getCredentialId())
                    .username(credDto.getUsername())
                    .password(credDto.getPassword())
                    .roleBasedAuthority(credDto.getRoleBasedAuthority())
                    .isEnabled(credDto.getIsEnabled())
                    .isAccountNonExpired(credDto.getIsAccountNonExpired())
                    .isAccountNonLocked(credDto.getIsAccountNonLocked())
                    .isCredentialsNonExpired(credDto.getIsCredentialsNonExpired())
                    .user(user)
                    .build();
            
            user.setCredential(credential);
        }
        
        return user;
    }
}