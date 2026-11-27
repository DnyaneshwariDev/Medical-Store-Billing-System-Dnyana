package com.medical.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.medical.entity.Bill;

public interface BillRepository extends JpaRepository<Bill, Long> {
	List<Bill> findTop10ByOrderByBillDateDesc();

	long countByBillDateBetween(LocalDateTime start, LocalDateTime end);
	long countByBillDateGreaterThanEqualAndBillDateLessThan(
			LocalDateTime start,
			LocalDateTime end);

	@Query("""
			SELECT COALESCE(SUM(b.totalAmount), 0)
			FROM Bill b
			WHERE b.billDate >= :start AND b.billDate < :end
			""")
	Double sumTotalAmountByBillDateBetween(
			@Param("start") LocalDateTime start,
			@Param("end") LocalDateTime end);

	@Query(value = """
			SELECT EXTRACT(MONTH FROM bill_date)::integer AS month,
			       COALESCE(SUM(total_amount), 0) AS total
			FROM bills
			WHERE bill_date IS NOT NULL
			GROUP BY EXTRACT(MONTH FROM bill_date)
			""", nativeQuery = true)
	List<Object[]> sumTotalAmountByMonth();

	@Query("""
			SELECT b.id AS id,
			       b.billDate AS billDate,
			       b.totalAmount AS totalAmount,
			       c.customerName AS customerName
			FROM Bill b
			LEFT JOIN b.customer c
			ORDER BY b.billDate DESC, b.id DESC
			""")
	List<RecentBillProjection> findRecentBills(Pageable pageable);

	@Query("""
			SELECT b FROM Bill b
			WHERE (:customer='' OR LOWER(b.customer.customerName)
			LIKE LOWER(CONCAT('%',:customer,'%')))
			AND (:type='' OR b.billType=:type)
			""")
	List<Bill> searchBills(
			@Param("customer") String customer,
			@Param("type") String type);

	List<Bill> findByCustomer_Id(Long customerId);

	interface RecentBillProjection {
		Long getId();
		java.time.LocalDateTime getBillDate();
		Double getTotalAmount();
		String getCustomerName();
	}
}
