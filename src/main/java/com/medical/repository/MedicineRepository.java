package com.medical.repository;

import com.medical.entity.Medicine;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {

	List<Medicine> findByQuantityLessThan(int i);
	List<Medicine> findByQuantityLessThanOrderByQuantityAscIdAsc(
			int quantity,
			Pageable pageable);
	List<Medicine> findByExpiryDateBetween(
            LocalDate startDate,
            LocalDate endDate);
	List<Medicine> findByExpiryDateBetweenOrderByExpiryDateAscIdAsc(
			LocalDate startDate,
			LocalDate endDate,
			Pageable pageable);

	long countByQuantityLessThan(int quantity);

	@Query("""
			SELECT COUNT(m),
			       COALESCE(SUM(CASE WHEN m.quantity IS NULL OR m.quantity = 0 THEN 1 ELSE 0 END), 0),
			       COALESCE(SUM(CASE WHEN m.quantity IS NOT NULL AND m.quantity <> 0 AND m.quantity <= 10 THEN 1 ELSE 0 END), 0),
			       COALESCE(SUM(CASE WHEN m.quantity > 10 THEN 1 ELSE 0 END), 0)
			FROM Medicine m
			""")
	Object[] getInventoryCounts();

	// Search medicine by exact name
    Optional<Medicine> findByMedicineName(String medicineName);

    // Search medicine names by prefix — paginated
    @Query("SELECT m FROM Medicine m WHERE " +
           "LOWER(m.medicineName) LIKE LOWER(CONCAT(:q, '%'))")
    Page<Medicine> searchMedicines(@Param("q") String query, Pageable pageable);
}