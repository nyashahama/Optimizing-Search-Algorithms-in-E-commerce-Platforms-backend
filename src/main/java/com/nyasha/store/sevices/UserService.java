package com.nyasha.store.sevices;

import com.nyasha.store.entities.User;
import com.nyasha.store.repositories.UserRepository;
import com.nyasha.store.utils.UserBST;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UserBST userBST = new UserBST();

    @PostConstruct
    public void initializeBST(){
        List<User> users = userRepository.findAll();
        for(User user:users){
            userBST.insert(user);
        }
    }

    // Create a new user
    public User createUser(User user) {
        user.setHashedPassword(passwordEncoder.encode(user.getHashedPassword()));
        User savedUser = userRepository.save(user);
        userBST.insert(savedUser);
        return savedUser;
    }

    // Update an existing user
    public User updateUser(Long id, User userDetails) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setName(userDetails.getName());
        user.setEmail(userDetails.getEmail());
        // Only hash and update the password if a new one is provided
        if (userDetails.getHashedPassword() != null && !userDetails.getHashedPassword().isEmpty()) {
            user.setHashedPassword(passwordEncoder.encode(userDetails.getHashedPassword()));
        }
        return userRepository.save(user);
    }

    // Read a user by ID
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    // Read all users
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Delete a user
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    /*public List<User> searchUsers(String searchTerm) {
        return userRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(searchTerm, searchTerm);
    }*/

    public List<User> searchUsers(String searchTerm) {
        try {
            System.out.println("Searching for: " + searchTerm);
            List<User> results = userBST.search(searchTerm.toLowerCase());
            System.out.println("Found results: " + results.size());
            return results;
        } catch (Exception e) {
            System.err.println("Search error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Search failed: " + e.getMessage());
        }
    }


}