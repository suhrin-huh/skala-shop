package com.skala.fund.repository;

import com.skala.fund.domain.Customer;
import com.skala.fund.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByCustomer(Customer customer);

    void deleteByCustomer(Customer customer);
}
