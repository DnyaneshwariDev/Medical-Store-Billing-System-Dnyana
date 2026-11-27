package com.medical.controller;

import com.medical.entity.Medicine;
import com.medical.repository.MedicineRepository;
import com.medical.service.MedicineService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/medicines")
public class MedicineController {

    private final MedicineService medicineService;
	private final MedicineRepository medicineRepository;

    
    public MedicineController(MedicineService medicineService, MedicineRepository medicineRepository) {
		super();
		this.medicineService = medicineService;
		this.medicineRepository = medicineRepository;
	}

	@PostMapping
    public Medicine addMedicine(@RequestBody Medicine medicine) {
        return medicineService.addMedicine(medicine);
    }

    @GetMapping
    public List<Medicine> getAllMedicines() {
        return medicineService.getAllMedicines();
    }

    /**
     * Search medicines with pagination.
     * GET /api/medicines/search?q=para&page=0&size=20
     * Returns a Spring Page object: { content: [...], totalElements, totalPages, ... }
     */
    @GetMapping("/search")
    public Page<Medicine> searchMedicines(
            @RequestParam(name = "q", defaultValue = "") String q,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return medicineService.searchMedicines(q, page, size);
    }
    
    @GetMapping("/{id}")
    public Medicine getMedicineById(@PathVariable Long id) {
        return medicineService.getMedicineById(id);
    }

    @PutMapping("/{id}")
    public Medicine updateMedicine(@PathVariable Long id,
                                   @RequestBody Medicine medicine) {
        return medicineService.updateMedicine(id, medicine);
    }
    @GetMapping("/schedule/{medicineName}")
    public String getSchedule(
    @PathVariable String medicineName){

        return medicineService.getMedicineSchedule(medicineName);

    }
    @DeleteMapping("/{id}")
    public String deleteMedicine(@PathVariable Long id) {
        medicineService.deleteMedicine(id);
        return "Medicine deleted successfully";
    }
}