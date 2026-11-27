package com.medical.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.medical.entity.Purchase;

public interface PurchaseRepository
        extends JpaRepository<Purchase, Long> {

    @Query("""
            SELECT COALESCE(SUM(p.totalAmount), 0)
            FROM Purchase p
            WHERE p.purchaseDate = :purchaseDate
            """)
    Double sumTotalAmountByPurchaseDate(@Param("purchaseDate") LocalDate purchaseDate);

    @Query(value = """
            SELECT EXTRACT(MONTH FROM purchase_date)::integer AS month,
                   COALESCE(SUM(total_amount), 0) AS total
            FROM purchases
            WHERE purchase_date IS NOT NULL
            GROUP BY EXTRACT(MONTH FROM purchase_date)
            """, nativeQuery = true)
    List<Object[]> sumTotalAmountByMonth();

    @Query("""
            SELECT p.invoiceNo AS invoiceNo,
                   s.supplierName AS supplierName,
                   p.purchaseDate AS purchaseDate,
                   p.totalAmount AS totalAmount
            FROM Purchase p
            LEFT JOIN p.supplier s
            ORDER BY p.purchaseDate DESC, p.id DESC
            """)
    List<RecentPurchaseProjection> findRecentPurchases(Pageable pageable);

    interface RecentPurchaseProjection {
        String getInvoiceNo();
        String getSupplierName();
        LocalDate getPurchaseDate();
        Double getTotalAmount();
    }
}