package com.txi.db.repository;

import com.txi.db.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {
    List<TransactionEntity> findByTxnDateBetween(LocalDate start, LocalDate end);
    List<TransactionEntity> findByCategoryIdAndTxnDateBetween(Long categoryId, LocalDate start, LocalDate end);
}
