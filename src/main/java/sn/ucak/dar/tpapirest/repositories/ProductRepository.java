package sn.ucak.dar.tpapirest.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.ucak.dar.tpapirest.entities.Product;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
}
