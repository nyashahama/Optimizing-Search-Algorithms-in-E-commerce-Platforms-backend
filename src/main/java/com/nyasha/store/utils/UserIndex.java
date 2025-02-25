package com.nyasha.store.utils;

import com.nyasha.store.entities.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

public class UserIndex {

    // Fast insertion and exact lookup indexes using HashMap
    private final HashMap<String, List<User>> fastIndexByName = new HashMap<>();
    private final HashMap<String, List<User>> fastIndexByEmail = new HashMap<>();

    // Sorted indexes using TreeMap for efficient prefix/range searches
    private final TreeMap<String, List<User>> sortedIndexByName = new TreeMap<>();
    private final TreeMap<String, List<User>> sortedIndexByEmail = new TreeMap<>();

    /**
     * Inserts a user into both the fast (HashMap) and sorted (TreeMap) indexes.
     */
    public synchronized void insert(User user) {
        String nameKey = user.getName().toLowerCase();
        String emailKey = user.getEmail().toLowerCase();

        // Update the HashMap indexes for fast insertion.
        fastIndexByName.computeIfAbsent(nameKey, k -> new ArrayList<>()).add(user);
        fastIndexByEmail.computeIfAbsent(emailKey, k -> new ArrayList<>()).add(user);

        // Update the TreeMap indexes for prefix searches.
        sortedIndexByName.computeIfAbsent(nameKey, k -> new ArrayList<>()).add(user);
        sortedIndexByEmail.computeIfAbsent(emailKey, k -> new ArrayList<>()).add(user);
    }

    /**
     * Removes a user from both the fast and sorted indexes.
     */
    public synchronized void remove(User user) {
        String nameKey = user.getName().toLowerCase();
        String emailKey = user.getEmail().toLowerCase();

        // Remove from fastIndexByName.
        List<User> nameList = fastIndexByName.get(nameKey);
        if (nameList != null) {
            nameList.remove(user);
            if (nameList.isEmpty()) {
                fastIndexByName.remove(nameKey);
            }
        }
        // Remove from fastIndexByEmail.
        List<User> emailList = fastIndexByEmail.get(emailKey);
        if (emailList != null) {
            emailList.remove(user);
            if (emailList.isEmpty()) {
                fastIndexByEmail.remove(emailKey);
            }
        }
        // Remove from sortedIndexByName.
        List<User> sortedNameList = sortedIndexByName.get(nameKey);
        if (sortedNameList != null) {
            sortedNameList.remove(user);
            if (sortedNameList.isEmpty()) {
                sortedIndexByName.remove(nameKey);
            }
        }
        // Remove from sortedIndexByEmail.
        List<User> sortedEmailList = sortedIndexByEmail.get(emailKey);
        if (sortedEmailList != null) {
            sortedEmailList.remove(user);
            if (sortedEmailList.isEmpty()) {
                sortedIndexByEmail.remove(emailKey);
            }
        }
    }

    /**
     * Updates a user in the index.
     *
     * Since the user's name and/or email might change, we use the old keys to remove the
     * previous entry, then insert the updated user.
     *
     * @param oldName   The user's name before update.
     * @param oldEmail  The user's email before update.
     * @param updatedUser The user object after update.
     */
    public synchronized void update(String oldName, String oldEmail, User updatedUser) {
        // Remove user from the indexes using the old keys.
        String oldNameKey = oldName.toLowerCase();
        String oldEmailKey = oldEmail.toLowerCase();

        // Remove from fastIndexByName.
        List<User> oldNameList = fastIndexByName.get(oldNameKey);
        if (oldNameList != null) {
            oldNameList.remove(updatedUser);
            if (oldNameList.isEmpty()) {
                fastIndexByName.remove(oldNameKey);
            }
        }
        // Remove from fastIndexByEmail.
        List<User> oldEmailList = fastIndexByEmail.get(oldEmailKey);
        if (oldEmailList != null) {
            oldEmailList.remove(updatedUser);
            if (oldEmailList.isEmpty()) {
                fastIndexByEmail.remove(oldEmailKey);
            }
        }
        // Remove from sortedIndexByName.
        List<User> sortedOldNameList = sortedIndexByName.get(oldNameKey);
        if (sortedOldNameList != null) {
            sortedOldNameList.remove(updatedUser);
            if (sortedOldNameList.isEmpty()) {
                sortedIndexByName.remove(oldNameKey);
            }
        }
        // Remove from sortedIndexByEmail.
        List<User> sortedOldEmailList = sortedIndexByEmail.get(oldEmailKey);
        if (sortedOldEmailList != null) {
            sortedOldEmailList.remove(updatedUser);
            if (sortedOldEmailList.isEmpty()) {
                sortedIndexByEmail.remove(oldEmailKey);
            }
        }
        // Now insert the updated user with the new keys.
        insert(updatedUser);
    }

    /**
     * Searches for users whose name or email starts with the given prefix.
     * This method uses the sorted indexes (TreeMap) to efficiently retrieve matching entries.
     */
    public List<User> search(String searchTerm) {
        String prefix = searchTerm.toLowerCase();
        Set<User> results = new HashSet<>();

        // Search by name prefix.
        results.addAll(searchByPrefix(sortedIndexByName, prefix));
        // Search by email prefix.
        results.addAll(searchByPrefix(sortedIndexByEmail, prefix));

        return new ArrayList<>(results);
    }

    /**
     * Helper method that searches a TreeMap index for keys starting with the given prefix.
     */
    private List<User> searchByPrefix(TreeMap<String, List<User>> tree, String prefix) {
        List<User> matches = new ArrayList<>();
        NavigableMap<String, List<User>> tailMap = tree.tailMap(prefix, true);
        for (String key : tailMap.keySet()) {
            if (!key.startsWith(prefix)) {
                break; // We've moved past the matching keys.
            }
            matches.addAll(tailMap.get(key));
        }
        return matches;
    }
    //best practices
}
