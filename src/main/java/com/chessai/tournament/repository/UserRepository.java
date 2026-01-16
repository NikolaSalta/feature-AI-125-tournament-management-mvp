package com.chessai.tournament.repository;

import com.chessai.tournament.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Репозиторий для работы с пользователями.
 * 
 * NOTE: N+1 Query Problem решается через EAGER fetch в User.roles.
 * Для production можно использовать @EntityGraph или @Query с JOIN FETCH,
 * но для демонстрации используем простой подход.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Найти пользователя по username
     */
    Optional<User> findByUsername(String username);

    /**
     * Найти пользователя по email
     */
    Optional<User> findByEmail(String email);

    /**
     * Найти пользователя по username или email.
     * Используется при аутентификации.
     */
    Optional<User> findByUsernameOrEmail(String username, String email);

    /**
     * Проверить существование пользователя по username
     */
    boolean existsByUsername(String username);

    /**
     * Проверить существование пользователя по email
     */
    boolean existsByEmail(String email);
}



