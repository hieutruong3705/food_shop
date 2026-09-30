package com.example.shop.repository;

import com.example.shop.domain.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Voucher> {
    Voucher findByCode(String code);
    /** Codes of the vouchers whose scope lists the category; these rows block deleting it. */
    @org.springframework.data.jpa.repository.Query("select v.code from Voucher v join v.categories c where c.id = :id order by v.code")
    java.util.List<String> codesByCategory(@org.springframework.data.repository.query.Param("id") long id);
    @org.springframework.data.jpa.repository.Query("select v.code from Voucher v join v.brands b where b.id = :id order by v.code")
    java.util.List<String> codesByBrand(@org.springframework.data.repository.query.Param("id") long id);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select v from Voucher v where v.code = :code")
    Voucher lockByCode(@org.springframework.data.repository.query.Param("code") String code);
}
