package com.example.shop.api.service;

import com.example.shop.domain.Order;
import com.example.shop.repository.VoucherRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

/** Gives a voucher use back when the order that spent it is cancelled or its payment fails or expires. */
@Service
public class VoucherUsage {
    private final VoucherRepository vouchers;
    @PersistenceContext private EntityManager entityManager;

    public VoucherUsage(VoucherRepository vouchers) {
        this.vouchers = vouchers;
    }

    /**
     * Callers close an order exactly once (a repeated CANCELLED is ignored before it gets here), so
     * each order gives its use back once. A voucher deleted since is skipped.
     */
    @Transactional
    public void refund(Order order) {
        String code = order.getVoucherCode();
        if (code == null || code.isBlank()) return;
        var voucher = vouchers.lockByCode(code.trim().toUpperCase(Locale.ROOT));
        if (voucher == null) return;
        entityManager.refresh(voucher, LockModeType.PESSIMISTIC_WRITE);
        voucher.setUsedCount(Math.max(0, voucher.getUsedCount() - 1));
    }
}
