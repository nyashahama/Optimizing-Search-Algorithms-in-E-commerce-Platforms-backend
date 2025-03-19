package com.nyasha.store.services;

import com.nyasha.store.entities.User;
import com.nyasha.store.repositories.UserRepository;
import com.nyasha.store.utils.UserIndex;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Combined in-memory index.
    private final UserIndex userIndex = new UserIndex();

    @PostConstruct
    public void initializeIndex() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            userIndex.insert(user);
        }
        logger.info("User index initialized with {} users", users.size());
    }

    // Create a new user, save to repository, and update the index.
    public User createUser(User user) {
        user.setHashedPassword(passwordEncoder.encode(user.getHashedPassword()));
        User savedUser = userRepository.save(user);
        userIndex.insert(savedUser);
        logger.info("Created user with id {}", savedUser.getUserId());
        return savedUser;
    }

    /**
     * Authenticate user by email and password.
     * @param email the user email.
     * @param password the plain text password.
     * @return the authenticated user, if credentials are valid.
     */
    public Optional<User> authenticateUser(String email, String password) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (passwordEncoder.matches(password, user.getHashedPassword())) {
                logger.info("User authenticated with id {}", user.getUserId());
                return Optional.of(user);
            }
        }
        logger.warn("Authentication failed for email: {}", email);
        return Optional.empty();
    }

    // Update an existing user.
    // Capture old name/email values before updating, then update the index.
    public User updateUser(Long id, User userDetails) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        String oldName = user.getName();
        String oldEmail = user.getEmail();

        // Update user details.
        user.setName(userDetails.getName());
        user.setEmail(userDetails.getEmail());
        if (userDetails.getHashedPassword() != null && !userDetails.getHashedPassword().isEmpty()) {
            user.setHashedPassword(passwordEncoder.encode(userDetails.getHashedPassword()));
        }
        User updatedUser = userRepository.save(user);

        // Update the in-memory index.
        userIndex.update(oldName, oldEmail, updatedUser);
        logger.info("Updated user with id {}", updatedUser.getUserId());
        return updatedUser;
    }

    // Read a user by ID.
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    // Read all users.
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Delete a user.
    public void deleteUser(Long id) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            // Remove from index first.
            userIndex.remove(user);
            userRepository.deleteById(id);
            logger.info("Deleted user with id {}", id);
        } else {
            logger.warn("Attempted to delete non-existing user with id {}", id);
        }
    }

    // Search users using the combined index.
    public List<User> searchUsers(String searchTerm) {
        try {
            logger.info("Searching for users with search term: {}", searchTerm);
            List<User> results = userIndex.search(searchTerm);
            logger.info("Found {} users matching search term '{}'", results.size(), searchTerm);
            return results;
        } catch (Exception e) {
            logger.error("Search error for term '{}': {}", searchTerm, e.getMessage(), e);
            throw new RuntimeException("Search failed: " + e.getMessage());
        }
    }
}




/*package com.nyasha.store.sevices;

import com.nyasha.store.entities.User;
import com.nyasha.store.repositories.UserRepository;
import com.nyasha.store.utils.UserIndex;
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

    // Using the best-practice, self-balancing index implementation.
    private UserIndex userIndex = new UserIndex();

    @PostConstruct
    public void initializeIndex() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            userIndex.insert(user);
        }
    }

    // Create a new user and add it to the index.
    public User createUser(User user) {
        user.setHashedPassword(passwordEncoder.encode(user.getHashedPassword()));
        User savedUser = userRepository.save(user);
        userIndex.insert(savedUser);
        return savedUser;
    }

    // Update an existing user.
    // NOTE: If updating fields that are indexed (name or email), you should also update the index.
    public User updateUser(Long id, User userDetails) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setName(userDetails.getName());
        user.setEmail(userDetails.getEmail());
        // Only hash and update the password if a new one is provided.
        if (userDetails.getHashedPassword() != null && !userDetails.getHashedPassword().isEmpty()) {
            user.setHashedPassword(passwordEncoder.encode(userDetails.getHashedPassword()));
        }
        User updatedUser = userRepository.save(user);
        // Optionally update the index here if your use-case requires it.
        return updatedUser;
    }

    // Read a user by ID.
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    // Read all users.
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Delete a user.
    // NOTE: You may want to also remove the user from the index.
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    // Search users using the self-balancing UserIndex for efficient prefix search.
    public List<User> searchUsers(String searchTerm) {
        try {
            System.out.println("Searching for: " + searchTerm);
            List<User> results = userIndex.search(searchTerm.toLowerCase());
            System.out.println("Found results: " + results.size());
            return results;
        } catch (Exception e) {
            System.err.println("Search error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Search failed: " + e.getMessage());
        }
    }
}*/

//incase i encounter errors:
/*package com.nyasha.store.sevices;

import com.nyasha.store.entities.User;
import com.nyasha.store.repositories.UserRepository;
import com.nyasha.store.utils.UserBST;
import com.nyasha.store.utils.UserIndex;
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

    private UserIndex userIndex;

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

   /*public List<User> searchUsers(String searchTerm) {
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


}*/
