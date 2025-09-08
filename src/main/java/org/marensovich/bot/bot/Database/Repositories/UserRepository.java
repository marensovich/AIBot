package org.marensovich.bot.bot.Database.Repositories;

import org.marensovich.bot.bot.Database.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> getUserByUserId(Long userId);

    User findUserByUserId(Long userId);
    boolean existsUserByUserId(Long userId);

}
