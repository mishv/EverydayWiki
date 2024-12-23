package com.amazing.EverydayWiki.database;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User saveUser(Long chatID, String username) {
        Optional<User> existingUser = Optional.ofNullable(userRepository.findByChatID(chatID));
        if (existingUser.isEmpty()) {
            User newUser = new User(chatID, username);
            return userRepository.save(newUser);
        }
        return existingUser.get();
    }

    public Optional<User> getUserByChatId(Long chatID) {
        return Optional.ofNullable(userRepository.findByChatID(chatID));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
