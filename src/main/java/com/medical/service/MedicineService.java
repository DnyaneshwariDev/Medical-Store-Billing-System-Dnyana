package com.medical.service;

import com.medical.entity.GstMaster;
import com.medical.entity.Medicine;
import com.medical.entity.ScheduleMaster;
import com.medical.repository.GstRepository;
import com.medical.repository.MedicineRepository;
import com.medical.repository.ScheduleRepository;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@Service
public class MedicineService {
    private static final int DEFAULT_SEARCH_PAGE_SIZE = 20;
    private static final int MAX_SEARCH_PAGE_SIZE = 25;

	@Autowired
	private ScheduleRepository repository;
	@Autowired
	private GstRepository gstRepository;

    private final MedicineRepository medicineRepository;

    public MedicineService(MedicineRepository medicineRepository) {
        this.medicineRepository = medicineRepository;
    }

    public Medicine addMedicine(Medicine medicine) {

        Medicine savedMedicine = medicineRepository.save(medicine);

        // GST Master Update
        if (!gstRepository.existsByMedicine_MedicineName(savedMedicine.getMedicineName())) {

            GstMaster gst = new GstMaster();

            gst.setMedicine(savedMedicine);

            gst.setGstPercentage(savedMedicine.getGstPercent());

            gstRepository.save(gst);
        }
        // Schedule Master Update
        if (!repository.existsByMedicine_MedicineName(savedMedicine.getMedicineName())) {

            ScheduleMaster schedule = new ScheduleMaster();

            schedule.setMedicine(savedMedicine);

            schedule.setScheduleType(savedMedicine.getScheduleType());

            repository.save(schedule);
        }

        return savedMedicine;
    }

    public List<Medicine> getAllMedicines() {
        return medicineRepository.findAll();
    }

    public Page<Medicine> searchMedicines(String query, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0
                ? DEFAULT_SEARCH_PAGE_SIZE
                : Math.min(size, MAX_SEARCH_PAGE_SIZE);
        PageRequest pageRequest = PageRequest.of(safePage, safeSize);
        String normalizedQuery = query == null ? "" : query.trim();

        if (normalizedQuery.isEmpty()) {
            return Page.empty(pageRequest);
        }

        return medicineRepository.searchMedicines(
                normalizedQuery,
                pageRequest
        );
    }

    public Medicine getMedicineById(Long id) {
        return medicineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Medicine not found"));
    }

    public Medicine updateMedicine(Long id, Medicine medicine) {

        Medicine existing = getMedicineById(id);

        existing.setMedicineName(medicine.getMedicineName());
        existing.setCompanyName(medicine.getCompanyName());
        existing.setBatchNo(medicine.getBatchNo());
        existing.setUnit(medicine.getUnit());
        existing.setMrp(medicine.getMrp());
        existing.setPurchasePrice(medicine.getPurchasePrice());
        existing.setOpeningStock(medicine.getOpeningStock());
        existing.setQuantity(medicine.getQuantity());
        existing.setGstPercent(medicine.getGstPercent());
        existing.setScheduleType(medicine.getScheduleType());
        existing.setExpiryDate(medicine.getExpiryDate());
        existing.setMarginPercent(medicine.getMarginPercent());
        existing.setContents(medicine.getContents());
        existing.setSymptoms(medicine.getSymptoms());

        Medicine savedMedicine = medicineRepository.save(existing);

        // GST Master Update
        GstMaster gst = gstRepository
                .findByMedicine_MedicineName(savedMedicine.getMedicineName())
                .orElse(new GstMaster());

        gst.setMedicine(savedMedicine);
        gst.setGstPercentage(savedMedicine.getGstPercent());

        gstRepository.save(gst);

        // Schedule Master Update
        ScheduleMaster schedule = repository
                .findByMedicine_MedicineName(savedMedicine.getMedicineName())
                .orElse(new ScheduleMaster());

        schedule.setMedicine(savedMedicine);
        schedule.setScheduleType(savedMedicine.getScheduleType());

        repository.save(schedule);

        return savedMedicine;
    }
    public String getMedicineSchedule(String medicineName){

        return repository
                .findByMedicine_MedicineName(medicineName)
                .map(ScheduleMaster::getScheduleType)
                .orElse("");

    }
    @Transactional
    public void deleteMedicine(Long id) {

        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Medicine not found"));

        // Delete Schedule Master
        repository.findByMedicine_MedicineName(medicine.getMedicineName())
                .ifPresent(repository::delete);

        // Delete GST Master
        gstRepository.findByMedicine_MedicineName(medicine.getMedicineName())
                .ifPresent(gstRepository::delete);

        // Delete Medicine
        medicineRepository.delete(medicine);
    }
}