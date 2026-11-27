package com.medical.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.medical.dto.PurchaseItemRequest;
import com.medical.dto.PurchaseRequest;
import com.medical.entity.Medicine;
import com.medical.entity.Purchase;
import com.medical.entity.PurchaseItem;
import com.medical.entity.Supplier;
import com.medical.repository.MedicineRepository;
import com.medical.repository.PurchaseRepository;
import com.medical.repository.SupplierRepository;

import jakarta.transaction.Transactional;

@Service
public class PurchaseService {

    @Autowired
    private PurchaseRepository purchaseRepository;
    @Autowired
  private SupplierRepository supplierRepository;
    @Autowired
  private MedicineRepository medicineRepository;

    public List<Purchase> getAll() {
        return purchaseRepository.findAll();
    }

    @Transactional
    public Purchase createPurchase(PurchaseRequest request) {
        List<PurchaseItem> itemList = new ArrayList<>();

        Supplier supplier =
            supplierRepository.findById(
                request.getSupplierId())
                .orElseThrow();
        double total = 0;
        Purchase purchase = new Purchase();

        purchase.setSupplier(supplier);
        purchase.setInvoiceNo(request.getInvoiceNo());
        purchase.setBatchNo(request.getBatchNo());
        purchase.setPurchaseDate(LocalDate.parse(request.getPurchaseDate()));

        for(PurchaseItemRequest itemRequest :
                request.getItems()) {

            Medicine medicine =
                medicineRepository.findById(
                    itemRequest.getMedicineId())
                    .orElseThrow();

            PurchaseItem item =
                new PurchaseItem();

            item.setPurchase(purchase);

            item.setMedicine(medicine);

            item.setQuantity(
                itemRequest.getQuantity());

            item.setPurchaseRate(
                itemRequest.getPurchaseRate());

            item.setSaleRate(
                itemRequest.getSaleRate());
            item.setBatchNo(itemRequest.getBatchNo());

            item.setGst(itemRequest.getGst());

            item.setExpiryDate(
                LocalDate.parse(itemRequest.getExpiryDate())
            );

            double amount =
                itemRequest.getQuantity()
                * itemRequest.getPurchaseRate();

            item.setAmount(amount);
            total += amount;
            itemList.add(item);
        }

        purchase.setItems(itemList);
        purchase.setTotalAmount(total);

        Purchase savedPurchase = purchaseRepository.save(purchase);

        for (PurchaseItem item : savedPurchase.getItems()) {
            Medicine medicine = item.getMedicine();
            medicine.setPurchasePrice(item.getPurchaseRate());
            medicine.setMrp(item.getSaleRate());
            medicine.setQuantity(
                medicine.getQuantity() + item.getQuantity());
            medicine.setBatchNo(item.getBatchNo());
            medicine.setGstPercent(item.getGst());
            medicine.setExpiryDate(item.getExpiryDate());

            medicineRepository.save(medicine);
        }

        supplier.setBalance(
            supplier.getBalance() == null
                ? total
                : supplier.getBalance() + total);
        supplierRepository.save(supplier);

        return savedPurchase;
    }
    public Purchase save(Purchase purchase) {
        return purchaseRepository.save(purchase);
    }

    public Purchase get(Long id) {
        return purchaseRepository.findById(id).orElse(null);
    }

    public void delete(Long id) {
        purchaseRepository.deleteById(id);
    }
}