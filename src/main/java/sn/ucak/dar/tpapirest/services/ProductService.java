package sn.ucak.dar.tpapirest.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.ucak.dar.tpapirest.dto.ProductRequest;
import sn.ucak.dar.tpapirest.dto.ProductResponse;
import sn.ucak.dar.tpapirest.entities.Product;
import sn.ucak.dar.tpapirest.exceptions.BadRequestException;
import sn.ucak.dar.tpapirest.exceptions.ResourceNotFoundException;
import sn.ucak.dar.tpapirest.repositories.ProductRepository;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ValidationUtils validationUtils;

    public ProductService(ProductRepository productRepository, ValidationUtils validationUtils) {
        this.productRepository = productRepository;
        this.validationUtils = validationUtils;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
            .stream()
            .map(ProductResponse::fromEntity)
            .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable"));
        return ProductResponse.fromEntity(product);
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        if (request == null) {
            throw new BadRequestException("Corps de requête obligatoire");
        }
        Map<String, String> errors = new LinkedHashMap<>();
        String cleanName = validationUtils.validateAndNormalizeName(request.getName(), errors);
        String validDescription = validationUtils.validateDescription(request.getDescription(), errors);
        BigDecimal validPrice = validationUtils.validatePrice(request.getPrice(), errors);
        validationUtils.throwIfErrors(errors);

        Product product = new Product(cleanName, validDescription, validPrice);
        Product saved = productRepository.save(product);
        return ProductResponse.fromEntity(saved);
    }

    @Transactional
    public ProductResponse updateProduct(UUID id, ProductRequest request) {
        if (request == null) {
            throw new BadRequestException("Corps de requête obligatoire");
        }
        Product existing = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable"));

        Map<String, String> errors = new LinkedHashMap<>();
        String cleanName = validationUtils.validateAndNormalizeName(request.getName(), errors);
        String validDescription = validationUtils.validateDescription(request.getDescription(), errors);
        BigDecimal validPrice = validationUtils.validatePrice(request.getPrice(), errors);
        validationUtils.throwIfErrors(errors);

        existing.setName(cleanName);
        existing.setDescription(validDescription);
        existing.setPrice(validPrice);

        Product updated = productRepository.save(existing);
        return ProductResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteProduct(UUID id) {
        Product existing = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable"));
        productRepository.delete(existing);
    }
}
