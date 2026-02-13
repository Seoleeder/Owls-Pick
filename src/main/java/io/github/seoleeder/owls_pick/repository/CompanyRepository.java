package io.github.seoleeder.owls_pick.repository;

import io.github.seoleeder.owls_pick.entity.game.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    List<Company> findByNameIn(List<String> names);
}
