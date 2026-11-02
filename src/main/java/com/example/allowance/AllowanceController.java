package com.example.allowance;

import jakarta.validation.Valid;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/allowances")
class AllowanceController {
    private final AllowanceRepository repository;

    AllowanceController(AllowanceRepository repository) { this.repository = repository; }

    @PostMapping
    ResponseEntity<AllowanceResponse> create(@Valid @RequestBody AllowanceRequest request, Principal user) {
        var allowance = repository.save(new Allowance(user.getName(), request)).response();
        return ResponseEntity.created(URI.create("/api/allowances/" + allowance.id())).body(allowance);
    }

    @GetMapping
    AllowancePage list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size, Principal user) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be nonnegative; size must be 1–100");
        }
        var results = repository.findByOwner(user.getName(), PageRequest.of(page, size, Sort.by("id")));
        return new AllowancePage(results.map(Allowance::response).getContent(), page, size,
                results.getTotalElements(), results.getTotalPages());
    }

    @GetMapping("/{id}")
    AllowanceResponse get(@PathVariable Long id, Principal user) {
        return owned(id, user).response();
    }

    @PutMapping("/{id}")
    @Transactional
    AllowanceResponse update(@PathVariable Long id, @Valid @RequestBody AllowanceRequest request, Principal user) {
        var allowance = owned(id, user);
        allowance.update(request);
        return allowance.response();
    }

    @DeleteMapping("/{id}")
    @Transactional
    ResponseEntity<Void> delete(@PathVariable Long id, Principal user) {
        repository.delete(owned(id, user));
        return ResponseEntity.noContent().build();
    }

    private Allowance owned(Long id, Principal user) {
        return repository.findByIdAndOwner(id, user.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Allowance not found"));
    }

    record AllowancePage(List<AllowanceResponse> content, int page, int size, long totalElements, int totalPages) {}
}
