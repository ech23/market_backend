package com.example.ogani.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Caching;

import com.example.ogani.entity.Category;
import com.example.ogani.entity.Image;
import com.example.ogani.entity.Product;
import com.example.ogani.exception.NotFoundException;
import com.example.ogani.model.request.CreateProductRequest;
import com.example.ogani.repository.CategoryRepository;
import com.example.ogani.repository.ImageRepository;
import com.example.ogani.repository.ProductRepository;
import com.example.ogani.service.ProductService;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Override
    @Cacheable(value = "productList")
    public List<Product> getList() {
        // TODO Auto-generated method stub
        return productRepository.findAll(Sort.by("id").descending());
    }

    @Override
    @Cacheable(value = "product", key = "#id")
    public Product getProduct(long id) {
        // TODO Auto-generated method stub
        Product product= productRepository.findById(id).orElseThrow(() -> new NotFoundException("Not Found Product With Id: " + id));

        return product;
    }
    

    @Override
    @CacheEvict(value = {"productList", "productNewest", "productByPrice", "productByCategory", "productRelated"}, allEntries = true)
    public Product createProduct(CreateProductRequest request) {
        // TODO Auto-generated method stub
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setQuantity(request.getQuantity());
        Category category = categoryRepository.findById(request.getCategoryId()).orElseThrow(()-> new NotFoundException("Not Found Category With Id: " + request.getCategoryId()));
        product.setCategory(category);

        Set<Image> images = new HashSet<>();
        for(long imageId: request.getImageIds()){
            Image image = imageRepository.findById(imageId).orElseThrow(() -> new NotFoundException("Not Found Image With Id: " + imageId));
            images.add(image);
        }
        product.setImages(images);
        productRepository.save(product);
        return product;
    }

    @Override
    @Caching(
        put = { @CachePut(value = "product", key = "#id") },
        evict = {
            @CacheEvict(value = "productList", allEntries = true),
            @CacheEvict(value = "productNewest", allEntries = true),
            @CacheEvict(value = "productByPrice", allEntries = true),
            @CacheEvict(value = "productByCategory", allEntries = true),
            @CacheEvict(value = "productRelated", allEntries = true)
        }
    )
    public Product updateProduct(long id, CreateProductRequest request) {
        // TODO Auto-generated method stub
        Product product= productRepository.findById(id).orElseThrow(() -> new NotFoundException("Not Found Product With Id: " + id));
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setQuantity(request.getQuantity());
        Category category = categoryRepository.findById(request.getCategoryId()).orElseThrow(()-> new NotFoundException("Not Found Category With Id: " + request.getCategoryId()));
        product.setCategory(category);

        Set<Image> images = new HashSet<>();
        for(long imageId: request.getImageIds()){
            Image image = imageRepository.findById(imageId).orElseThrow(() -> new NotFoundException("Not Found Image With Id: " + imageId));
            images.add(image);
        }
        product.setImages(images);
        productRepository.save(product);

        return product;
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = "product", key = "#id"),
        @CacheEvict(value = "productList", allEntries = true),
        @CacheEvict(value = "productNewest", allEntries = true),
        @CacheEvict(value = "productByPrice", allEntries = true),
        @CacheEvict(value = "productByCategory", allEntries = true),
        @CacheEvict(value = "productRelated", allEntries = true)
    })
    public void deleteProduct(long id) {
        // TODO Auto-generated method stub
        Product product= productRepository.findById(id).orElseThrow(() -> new NotFoundException("Not Found Product With Id: " + id));
        product.getImages().remove(this);
        productRepository.delete(product);
    }

    @Override
    @Cacheable(value = "productNewest", key = "#number")
    public List<Product> getListNewst(int number) {
        // TODO Auto-generated method stub
        List<Product> list = productRepository.getListNewest(number);
        return list;
    }

    @Override
    @Cacheable(value = "productByPrice")
    public List<Product> getListByPrice() {
        // TODO Auto-generated method stub
        return productRepository.getListByPrice();
    }

    @Override
    @Cacheable(value = "productRelated", key = "#id")
    public List<Product> findRelatedProduct(long id){
        List<Product> list = productRepository.findRelatedProduct(id);
        return list;

    }

    @Override
    @Cacheable(value = "productByCategory", key = "#id")
    public List<Product> getListProductByCategory(long id){
        List<Product> list =productRepository.getListProductByCategory(id);
        return list;
    }

    @Override
    @Cacheable(value = "productByPriceRange", key = "{#id, #min, #max}")
    public List<Product> getListByPriceRange(long id,int min, int max){
        List<Product> list =productRepository.getListProductByPriceRange(id, min, max);
        return list;
    }

    @Override
    @Cacheable(value = "productSearch", key = "#keyword")
    public List<Product> searchProduct(String keyword) {
        // TODO Auto-generated method stub
        List<Product> list = productRepository.searchProduct(keyword);
        return list;
    }


    
}
