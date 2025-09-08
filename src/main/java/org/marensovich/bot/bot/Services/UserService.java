package org.marensovich.bot.bot.Services;

import org.marensovich.bot.bot.Database.Models.User;
import org.marensovich.bot.bot.Database.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired private UserRepository userRepository;

    public boolean isUserExists(Long userId) {
        return userRepository.existsUserByUserId(userId);
    }

    public boolean isUserAdmin(Long userId) {
        return userRepository.getUserByUserId(userId).get().isAdmin();
    }

    public void createUser(Long userId) {
        User user = new User();
        user.setUserId(userId);
        userRepository.save(user);
    }


}
