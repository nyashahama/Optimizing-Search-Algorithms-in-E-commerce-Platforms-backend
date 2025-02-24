package com.nyasha.store.utils;

import com.nyasha.store.entities.User;
import java.util.ArrayList;
import java.util.List;

public class UserBST {
    private UserNode root;

    public UserBST() {
        this.root = null;
    }

    // Insert a user into the BST based on name
    public void insert(User user) {
        root = insertRec(root, user);
    }

    private UserNode insertRec(UserNode root, User user) {
        if (root == null) {
            return new UserNode(user);
        }
        // Organize BST by name (case-insensitive)
        if (user.getName().compareToIgnoreCase(root.user.getName()) < 0) {
            root.left = insertRec(root.left, user);
        } else if (user.getName().compareToIgnoreCase(root.user.getName()) > 0) {
            root.right = insertRec(root.right, user);
        }
        return root;
    }

    // Search for users by name or email containing the search term
    public List<User> search(String searchTerm) {
        List<User> result = new ArrayList<>();
        searchRec(root, searchTerm.toLowerCase(), result);
        return result;
    }

    private void searchRec(UserNode root, String searchTerm, List<User> result) {
        if (root == null) return; // Avoid NullPointerException

        // Check if name or email contains the search term (case-insensitive)
        if (root.user.getName().toLowerCase().startsWith(searchTerm) ||
                root.user.getEmail().toLowerCase().startsWith(searchTerm)) {
            result.add(root.user);
        }

        // Continue searching in left and right subtrees
        if (root.left != null) searchRec(root.left, searchTerm, result);
        if (root.right != null) searchRec(root.right, searchTerm, result);
    }

}