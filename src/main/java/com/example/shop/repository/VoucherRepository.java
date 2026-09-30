package com.example.shop.repository;

import com.example.shop.domain.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Voucher> {
    Voucher findByCode(String code);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select v from Voucher v where v.code = :code")
    Voucher lockByCode(@org.springframework.data.repository.query.Param("code") String code);
}
