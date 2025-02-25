package com.nyasha.store.sevices;

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

    // Use the combined index.
    private final UserIndex userIndex = new UserIndex();

    @PostConstruct
    public void initializeIndex() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            userIndex.insert(user);
        }
    }

    // Create a new user, save to repository, and update the index.
    public User createUser(User user) {
        user.setHashedPassword(passwordEncoder.encode(user.getHashedPassword()));
        User savedUser = userRepository.save(user);
        userIndex.insert(savedUser);
        return savedUser;
    }

    // Update an existing user.
    // Capture the old name/email values before updating, then update the index.
    public User updateUser(Long id, User userDetails) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Capture old keys before updating.
        String oldName = user.getName();
        String oldEmail = user.getEmail();

        // Update user details.
        user.setName(userDetails.getName());
        user.setEmail(userDetails.getEmail());
        if (userDetails.getHashedPassword() != null && !userDetails.getHashedPassword().isEmpty()) {
            user.setHashedPassword(passwordEncoder.encode(userDetails.getHashedPassword()));
        }
        User updatedUser = userRepository.save(user);

        // Update the index using the old keys.
        userIndex.update(oldName, oldEmail, updatedUser);
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
        if(optionalUser.isPresent()){
            User user = optionalUser.get();
            // Remove from index first.
            userIndex.remove(user);
            userRepository.deleteById(id);
        }
    }

    // Search users using the combined index.
    public List<User> searchUsers(String searchTerm) {
        try {
            System.out.println("Searching for: " + searchTerm);
            List<User> results = userIndex.search(searchTerm);
            System.out.println("Found results: " + results.size());
            return results;
        } catch (Exception e) {
            System.err.println("Search error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Search failed: " + e.getMessage());
        }
    }
    //best practices
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
