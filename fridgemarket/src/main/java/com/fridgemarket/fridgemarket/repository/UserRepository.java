package com.fridgemarket.fridgemarket.repository;
import com.fridgemarket.fridgemarket.DAO.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
