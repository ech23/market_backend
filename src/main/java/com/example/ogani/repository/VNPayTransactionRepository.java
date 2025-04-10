package com.example.ogani.repository;

import com.example.ogani.entity.VNPayTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VNPayTransactionRepository extends JpaRepository<VNPayTransaction, Long> {
    VNPayTransaction findByVnpTxnRef(String vnpTxnRef);
} 