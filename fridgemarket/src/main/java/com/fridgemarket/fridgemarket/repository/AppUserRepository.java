package com.fridgemarket.fridgemarket.repository;

import com.fridgemarket.fridgemarket.DAO.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<User, Long> {
    Optional<User> findByProviderAndUserid(String provider, String userid);
    Optional<User> findByUserid(String userid);
}
