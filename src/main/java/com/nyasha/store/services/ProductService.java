package com.nyasha.store.services;


import com.nyasha.store.entities.Product;
import com.nyasha.store.repositories.ProductRepository;
import com.nyasha.store.utils.ProductIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;


@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final ProductIndex productIndex;

    @Autowired
    public ProductService(ProductRepository productRepository, ProductIndex productIndex) {
        this.productRepository = productRepository;
        this.productIndex = productIndex;
    }

    // Create a product and add it to the product index
    public Product createProduct(Product product) {
        //product.setCreatedAt(LocalDateTime.now());
        try {
            Product savedProduct = productRepository.save(product);
            productIndex.insert(savedProduct);
            logger.info("Created product with id {}", savedProduct.getProductId());
            return savedProduct;
        } catch (Exception e) {
            logger.error("Error creating product: {}", e.getMessage(), e);
            throw new RuntimeException("Product creation failed: " + e.getMessage());
        }
    }

    // Get all products
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // Get product by ID
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }

    // Update a product and refresh the product index
    public Product updateProduct(Long id, Product productDetails) {
        try {
            Product existingProduct = productRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            // Capture the old state for index update
            Product oldProduct = new Product();
            oldProduct.setProductId(existingProduct.getProductId());
            oldProduct.setName(existingProduct.getName());
            oldProduct.setSku(existingProduct.getSku());
            oldProduct.setDescription(existingProduct.getDescription());
            oldProduct.setCategories(existingProduct.getCategories());

            // Apply updates
            existingProduct.setName(productDetails.getName());
            existingProduct.setDescription(productDetails.getDescription());
            existingProduct.setBasePrice(productDetails.getBasePrice());
            existingProduct.setSku(productDetails.getSku());
            existingProduct.setCategories(productDetails.getCategories());

            Product updatedProduct = productRepository.save(existingProduct);
            productIndex.update(oldProduct, updatedProduct);
            logger.info("Updated product with id {}", updatedProduct.getProductId());
            return updatedProduct;
        } catch (RuntimeException e) {
            logger.error("Error updating product with id {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Product update failed: " + e.getMessage());
        }
    }

    // Delete a product and remove it from the index
    public void deleteProduct(Long id) {
        try {
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            productIndex.remove(product);
            productRepository.delete(product);
            logger.info("Deleted product with id {}", id);
        } catch (RuntimeException e) {
            logger.error("Error deleting product with id {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Product deletion failed: " + e.getMessage());
        }
    }

    // Full-text search using the inverted index
    public Set<Product> searchByText(String query) {
        try {
            Set<Product> results = productIndex.searchByText(query);
            logger.info("Text search for '{}' returned {} results", query, results.size());
            return results;
        } catch (Exception e) {
            logger.error("Error during text search for '{}': {}", query, e.getMessage(), e);
            throw new RuntimeException("Text search failed: " + e.getMessage());
        }
    }

    // Prefix-based autocompletion
    public List<Product> autocomplete(String prefix) {
        try {
            List<Product> results = productIndex.searchByPrefix(prefix);
            logger.info("Autocomplete for '{}' returned {} results", prefix, results.size());
            return results;
        } catch (Exception e) {
            logger.error("Error during autocomplete for '{}': {}", prefix, e.getMessage(), e);
            throw new RuntimeException("Autocomplete failed: " + e.getMessage());
        }
    }

    // Get products by category
    public List<Product> getProductsByCategory(String categoryId) {
        return productIndex.searchByCategory(categoryId);
    }
}