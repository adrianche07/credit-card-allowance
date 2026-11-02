package com.example.allowance;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface AllowanceRepository extends JpaRepository<Allowance, Long> {
    Optional<Allowance> findByIdAndOwner(Long id, String owner);
    Page<Allowance> findByOwner(String owner, Pageable pageable);
}
