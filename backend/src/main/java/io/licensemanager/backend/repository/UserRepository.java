package io.licensemanager.backend.repository;

import io.licensemanager.backend.entity.Role;
import io.licensemanager.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(final String username);

    Boolean existsByUsername(final String username);

    Boolean existsByEmail(final String email);

    List<User> findAllByIsAccountActivatedByAdminTrueAndUsernameIsNot(final String username);

    Set<User> findAllByIdIn(final Set<Long> usersIds);

    Set<User> findAllByRolesContains(final Role role);

    List<User> findAllByIsAccountActivatedByAdminFalse();

    void deleteById(final Long userId);
}
