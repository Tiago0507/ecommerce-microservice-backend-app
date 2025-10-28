package com.selimhorri.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.selimhorri.app.domain.User;

public interface UserRepository extends JpaRepository<User, Integer> {
	
	@Query("SELECT u FROM User u LEFT JOIN FETCH u.credential WHERE u.userId = :id")
    Optional<User> findByIdWithCredential(@Param("id") Integer id);
    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.credential")
    List<User> findAllWithCredentials();
    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.credential c WHERE c.username = :username")
    Optional<User> findByCredentialUsername(@Param("username") String username);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE LOWER(u.email) = LOWER(:email)")
    boolean existsByEmailIgnoreCase(@Param("email") String email);
	
}
