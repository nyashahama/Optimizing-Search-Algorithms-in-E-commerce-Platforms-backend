package com.nyasha.store.utils;

import com.nyasha.store.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class UserIndex {

    private static final Logger logger = LoggerFactory.getLogger(UserIndex.class);

    // Fast lookup indexes using ConcurrentHashMap.
    // Instead of CopyOnWriteArrayList, we use synchronized lists for better write performance.
    private final ConcurrentMap<String, List<User>> fastIndexByName = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, List<User>> fastIndexByEmail = new ConcurrentHashMap<>();

    // Sorted indexes using ConcurrentSkipListMap for efficient prefix/range searches.
    private final ConcurrentSkipListMap<String, List<User>> sortedIndexByName = new ConcurrentSkipListMap<>();
    private final ConcurrentSkipListMap<String, List<User>> sortedIndexByEmail = new ConcurrentSkipListMap<>();

    /**
     * Helper method to get or create a synchronized list from the given map.
     */
    private List<User> getOrCreateList(ConcurrentMap<String, List<User>> map, String key) {
        return map.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>()));
    }

    /**
     * Inserts a user into both the fast (ConcurrentHashMap) and sorted (ConcurrentSkipListMap) indexes.
     */
    public void insert(User user) {
        String nameKey = user.getName().toLowerCase();
        String emailKey = user.getEmail().toLowerCase();

        getOrCreateList(fastIndexByName, nameKey).add(user);
        getOrCreateList(fastIndexByEmail, emailKey).add(user);
        getOrCreateList(sortedIndexByName, nameKey).add(user);
        getOrCreateList(sortedIndexByEmail, emailKey).add(user);

        logger.debug("Inserted user {} into indexes", user.getUserId());
    }

    /**
     * Removes a user from both the fast and sorted indexes.
     */
    public void remove(User user) {
        String nameKey = user.getName().toLowerCase();
        String emailKey = user.getEmail().toLowerCase();

        removeFromIndex(fastIndexByName, nameKey, user);
        removeFromIndex(fastIndexByEmail, emailKey, user);
        removeFromIndex(sortedIndexByName, nameKey, user);
        removeFromIndex(sortedIndexByEmail, emailKey, user);

        logger.debug("Removed user {} from indexes", user.getUserId());
    }

    /**
     * Updates a user in the index.
     * Since the user's name and/or email might change, we remove the user using the old keys and then insert the updated user.
     *
     * @param oldName     The user's name before update.
     * @param oldEmail    The user's email before update.
     * @param updatedUser The user object after update.
     */
    public void update(String oldName, String oldEmail, User updatedUser) {
        String oldNameKey = oldName.toLowerCase();
        String oldEmailKey = oldEmail.toLowerCase();

        // Remove using the old keys.
        removeFromIndex(fastIndexByName, oldNameKey, updatedUser);
        removeFromIndex(fastIndexByEmail, oldEmailKey, updatedUser);
        removeFromIndex(sortedIndexByName, oldNameKey, updatedUser);
        removeFromIndex(sortedIndexByEmail, oldEmailKey, updatedUser);

        // Insert the updated user with the new keys.
        insert(updatedUser);

        logger.debug("Updated user {} in indexes", updatedUser.getUserId());
    }

    /**
     * Searches for users whose name or email starts with the given prefix.
     * Uses the sorted indexes (ConcurrentSkipListMap) for efficient retrieval.
     */
    public List<User> search(String searchTerm) {
        String prefix = searchTerm.toLowerCase();
        Set<User> results = new HashSet<>();

        results.addAll(searchByPrefix(sortedIndexByName, prefix));
        results.addAll(searchByPrefix(sortedIndexByEmail, prefix));

        logger.debug("Search for prefix '{}' returned {} results", prefix, results.size());
        return new ArrayList<>(results);
    }

    /**
     * Helper method to remove a user from an index.
     */
    private void removeFromIndex(ConcurrentMap<String, List<User>> index, String key, User user) {
        List<User> list = index.get(key);
        if (list != null) {
            synchronized (list) {
                list.remove(user);
                if (list.isEmpty()) {
                    index.remove(key, list);
                }
            }
        }
    }

    /**
     * Helper method that searches a ConcurrentSkipListMap index for keys starting with the given prefix.
     */
    private List<User> searchByPrefix(ConcurrentSkipListMap<String, List<User>> map, String prefix) {
        List<User> matches = new ArrayList<>();
        NavigableMap<String, List<User>> tailMap = map.tailMap(prefix, true);
        for (Map.Entry<String, List<User>> entry : tailMap.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith(prefix)) {
                break;
            }
            synchronized (entry.getValue()) {
                matches.addAll(entry.getValue());
            }
        }
        return matches;
    }
}
