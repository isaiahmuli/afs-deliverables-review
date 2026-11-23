package com.example.payments;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentLinkRepository extends JpaRepository<PaymentLink, Long> {

    Optional<PaymentLink> findByInvoiceId(Long invoiceId);

    Optional<PaymentLink> findByCheckoutId(String checkoutId);
}
