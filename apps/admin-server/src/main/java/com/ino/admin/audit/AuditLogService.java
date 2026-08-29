package com.ino.admin.audit;

import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class AuditLogService implements AuditWriter {
    private final AuditLogRepository repository;
    private final Clock clock;

    AuditLogService(AuditLogRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(AuditCommand command) {
        repository.save(AuditLog.create(command, Instant.now(clock)));
    }

    @Transactional(readOnly = true)
    Page<AuditLog> findAccessHistory(Instant from, Instant to,
            int page, int size) {
        return repository.findAll((root, ignored, builder) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(builder.equal(root.get("action"), "AUTH_LOGIN"));
            predicates.add(builder.equal(root.get("result"), AuditResult.SUCCESS));
            predicates.add(builder.isNotNull(root.get("loginEmail")));
            if (from != null) predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), from));
            if (to != null) predicates.add(builder.lessThan(root.get("createdAt"), to));
            return builder.and(predicates.toArray(Predicate[]::new));
        }, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")
                .and(Sort.by(Sort.Direction.ASC, "id"))));
    }
}
