package com.nyasha.store.utils;

import com.nyasha.store.entities.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;

@Component
public class ProductIndex {
    private static final Logger logger = LoggerFactory.getLogger(ProductIndex.class);

    // Fast lookup indexes (exact matches)
    private final ConcurrentMap<String, List<Product>> fastIndexByName = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, List<Product>> fastIndexBySku = new ConcurrentHashMap<>();

    // Sorted indexes for prefix/range searches
    private final ConcurrentSkipListMap<String, List<Product>> sortedIndexByName = new ConcurrentSkipListMap<>();
    private final ConcurrentSkipListMap<String, List<Product>> sortedIndexBySku = new ConcurrentSkipListMap<>();

    // Inverted index for full-text search (term -> products)
    private final ConcurrentMap<String, Set<Product>> invertedIndex = new ConcurrentHashMap<>();

    // Category index (categoryId -> products)
    private final ConcurrentMap<String, List<Product>> categoryIndex = new ConcurrentHashMap<>();

    private final Object indexLock = new Object();

    /**
     * Helper method to get or create a synchronized list.
     */
    private List<Product> getOrCreateList(ConcurrentMap<String, List<Product>> map, String key) {
        return map.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>()));
    }

    /**
     * Inserts a product into all indexes.
     */
    public void insert(Product product) {
        // Index by name and SKU
        String nameKey = product.getName().toLowerCase();
        String skuKey = product.getSku().toLowerCase();
        try {
            synchronized (indexLock) {
                getOrCreateList(fastIndexByName, nameKey).add(product);
                getOrCreateList(fastIndexBySku, skuKey).add(product);
                getOrCreateList(sortedIndexByName, nameKey).add(product);
                getOrCreateList(sortedIndexBySku, skuKey).add(product);

                // Index categories
                product.getCategories().forEach(category ->
                        getOrCreateList(categoryIndex, category.getCategoryId().toString()).add(product)
                );

                // Build inverted index for full-text search
                indexTextFields(product);
            }
            logger.debug("Inserted product {} into indexes", product.getProductId());
        }catch (Exception e){
            logger.error("Error inserting product {}: {}", product.getProductId(), e.getMessage(), e);
        }

    }

    /**
     * Removes a product from all indexes.
     */
    public void remove(Product product) {
        String nameKey = product.getName().toLowerCase();
        String skuKey = product.getSku().toLowerCase();
        try {
            synchronized (indexLock) {
                removeFromIndex(fastIndexByName, nameKey, product);
                removeFromIndex(fastIndexBySku, skuKey, product);
                removeFromIndex(sortedIndexByName, nameKey, product);
                removeFromIndex(sortedIndexBySku, skuKey, product);

                // Remove from category index
                product.getCategories().forEach(category ->
                        removeFromIndex(categoryIndex, category.getCategoryId().toString(), product)
                );

                // Remove from inverted index
                removeFromInvertedIndex(product);
            }
            logger.debug("Removed product {} from indexes", product.getProductId());
        }catch (Exception e){
            logger.error("Error removing product {}: {}",product.getProductId(), e.getMessage(),e);
        }
    }

    /**
     * Updates a product in the index.
     */
    public void update(Product oldProduct, Product updatedProduct) {
        try {
            synchronized (indexLock) {
                remove(oldProduct);
                insert(updatedProduct);
            }
            logger.debug("Updated product {} in indexes", updatedProduct.getProductId());
        }catch (Exception e){
            logger.error("Error updating product {} in indexes: {}", updatedProduct.getProductId(), e.getMessage(), e);
        }
    }

    /**
     * Full-text search across product names and descriptions.
     */
    public Set<Product> searchByText(String query) {
        Set<Product> results = new HashSet<>();
        String[] terms = query.toLowerCase().split("\\W+");
        for (String term : terms) {
            Set<Product> matches = invertedIndex.getOrDefault(term, Collections.emptySet());
            results.addAll(matches);
        }
        logger.debug("Full-text search for '{}' returned {} results", query, results.size());
        return results;
    }

    /**
     * Prefix-based search for autocompletion.
     */
    public List<Product> searchByPrefix(String prefix) {
        String normalizedPrefix = prefix.toLowerCase();
        Set<Product> results = new HashSet<>();
        results.addAll(searchByPrefix(sortedIndexByName, normalizedPrefix));
        results.addAll(searchByPrefix(sortedIndexBySku, normalizedPrefix));
        logger.debug("Prefix search for '{}' returned {} results", prefix, results.size());
        return new ArrayList<>(results);
    }

    /**
     * Search products by category ID.
     */
    public List<Product> searchByCategory(String categoryId) {
        return categoryIndex.getOrDefault(categoryId, Collections.emptyList());
    }

    // --- Helper Methods ---
    private void indexTextFields(Product product) {
        String[] textFields = { product.getName(), product.getDescription() };
        for (String text : textFields) {
            String[] terms = text.toLowerCase().split("\\W+");
            for (String term : terms) {
                invertedIndex.computeIfAbsent(term, k -> ConcurrentHashMap.newKeySet())
                        .add(product);
            }
        }
    }

    private void removeFromInvertedIndex(Product product) {
        String[] textFields = { product.getName(), product.getDescription() };
        for (String text : textFields) {
            String[] terms = text.toLowerCase().split("\\W+");
            for (String term : terms) {
                Set<Product> products = invertedIndex.get(term);
                if (products != null) {
                    products.remove(product);
                    if (products.isEmpty()) {
                        invertedIndex.remove(term);
                    }
                }
            }
        }
    }

    private void removeFromIndex(ConcurrentMap<String, List<Product>> map, String key, Product product) {
        List<Product> list = map.get(key);
        if (list != null) {
            synchronized (list) {
                list.remove(product);
                if (list.isEmpty()) {
                    map.remove(key, list);
                }
            }
        }
    }

    private List<Product> searchByPrefix(ConcurrentSkipListMap<String, List<Product>> map, String prefix) {
        List<Product> matches = new ArrayList<>();
        NavigableMap<String, List<Product>> tailMap = map.tailMap(prefix, true);
        for (Map.Entry<String, List<Product>> entry : tailMap.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith(prefix)) break;
            synchronized (entry.getValue()) {
                matches.addAll(entry.getValue());
            }
        }
        return matches;
    }
}