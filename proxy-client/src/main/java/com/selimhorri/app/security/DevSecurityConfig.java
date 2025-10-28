package com.selimhorri.app.security;

import java.util.Collections;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Configuración de seguridad para entorno de DESARROLLO
 * Deshabilita autenticación para facilitar pruebas locales
 * Solo activa cuando profile = "dev"
 */
@Configuration
@EnableWebSecurity
@Profile("dev")
public class DevSecurityConfig extends WebSecurityConfigurerAdapter {
	
	@Override
	protected void configure(final HttpSecurity http) throws Exception {
		http
			.cors().disable()
			.csrf().disable()
			.authorizeRequests()
				.anyRequest().permitAll() // Permitir todo en desarrollo
			.and()
			.sessionManagement()
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			.and()
			.headers()
				.frameOptions()
				.sameOrigin(); // Permitir H2 console
	}
    
	/**
	 * Exponer un AuthenticationManager permisivo solo en dev para satisfacer dependencias
	 * y permitir autenticación sin validar credenciales. Esto evita que falle la creación
	 * de beans como AuthenticationServiceImpl cuando el perfil dev no registra proveedores reales.
	 */
	@Bean
	@Override
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return (Authentication authentication) -> {
			// Acepta cualquier usuario en dev y le asigna ROLE_USER
			return new UsernamePasswordAuthenticationToken(
				authentication.getPrincipal(),
				authentication.getCredentials(),
				Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
			);
		};
	}
	
}
