package com.aresus.cliper.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.aresus.cliper.model.token.Token;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {

    @Query("SELECT t FROM Token t ORDER BY t.id DESC")
    Token findFirstByOrderByIdDesc();
    Token findByAccessToken(String accessToken); 
}


