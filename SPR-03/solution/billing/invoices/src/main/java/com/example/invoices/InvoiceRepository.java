package com.example.invoices;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findAllByOrderByIdAsc();

    /** Locks the row so two status changes to the same invoice run one after the other. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Invoice> findForUpdateById(Long id);
}
