package com.medical.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.medical.entity.BillItem;

public interface BillItemRepository
extends JpaRepository<BillItem, Long> {
	@Query("""
			select coalesce(sum(b.quantity),0)
			from BillItem b
			where b.medicine.id=:medicineId
			""")
			Integer getSaleQty(Long medicineId);

	@Query("""
			SELECT COALESCE(SUM(
			    CASE
			        WHEN m.id IS NULL THEN COALESCE(bi.amount, 0.0)
			        ELSE COALESCE(m.purchasePrice, 0.0) * COALESCE(bi.quantity, 0)
			    END
			), 0.0)
			FROM BillItem bi
			JOIN bi.bill b
			LEFT JOIN bi.medicine m
			WHERE b.billDate >= :start AND b.billDate < :end
			""")
	Double sumCostByBillDateBetween(
			@Param("start") LocalDateTime start,
			@Param("end") LocalDateTime end);

	@Query("""
			SELECT m.medicineName AS medicineName,
			       SUM(bi.quantity) AS saleQuantity
			FROM BillItem bi
			JOIN bi.medicine m
			GROUP BY m.id, m.medicineName
			ORDER BY SUM(bi.quantity) DESC, m.medicineName ASC
			""")
	List<TopMedicineProjection> findTopSellingMedicines(Pageable pageable);

	interface TopMedicineProjection {
		String getMedicineName();
		Long getSaleQuantity();
	}

	List<BillItem> findByBillId(Long billId);
}