package sn.ucak.dar.tpapirest.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.ucak.dar.tpapirest.entities.Role;
import sn.ucak.dar.tpapirest.entities.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, UUID id);
    boolean existsByRole(Role role);
}
