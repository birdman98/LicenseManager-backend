package io.licensemanager.backend.repository;

import io.licensemanager.backend.entity.CustomerGroup;
import io.licensemanager.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CustomerGroupRepository extends JpaRepository<CustomerGroup, Long> {

    List<CustomerGroup> findAllByCreatorIs(final User creator);

    Set<CustomerGroup> findAllByCreatorIsAndNameIn(final User creator, final Set<String> names);

    Optional<CustomerGroup> findByCreatorIsAndName(final User creator, final String name);

    Optional<CustomerGroup> findByCreatorIsAndId(final User creator, final Long groupId);
}
