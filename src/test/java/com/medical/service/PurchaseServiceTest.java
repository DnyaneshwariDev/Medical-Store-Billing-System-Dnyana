package com.medical.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.medical.dto.PurchaseItemRequest;
import com.medical.dto.PurchaseRequest;
import com.medical.entity.Medicine;
import com.medical.entity.Purchase;
import com.medical.entity.Supplier;
import com.medical.repository.MedicineRepository;
import com.medical.repository.PurchaseRepository;
import com.medical.repository.SupplierRepository;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private MedicineRepository medicineRepository;

    @InjectMocks
    private PurchaseService purchaseService;

    @Test
    void savesPurchaseItemsBeforeUpdatingMedicineStock() {
        Supplier supplier = new Supplier();
        supplier.setBalance(20.0);
        Medicine medicine = new Medicine();
        medicine.setQuantity(5);

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(medicineRepository.findById(2L)).thenReturn(Optional.of(medicine));
        when(purchaseRepository.save(any(Purchase.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(medicineRepository.save(any(Medicine.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(supplierRepository.save(any(Supplier.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Purchase purchase = purchaseService.createPurchase(request());

        ArgumentCaptor<Purchase> savedPurchase = ArgumentCaptor.forClass(Purchase.class);
        InOrder saveOrder = inOrder(purchaseRepository, medicineRepository);
        saveOrder.verify(purchaseRepository).save(savedPurchase.capture());
        saveOrder.verify(medicineRepository).save(medicine);

        assertEquals(1, savedPurchase.getValue().getItems().size());
        assertEquals("BATCH-01", savedPurchase.getValue().getBatchNo());
        assertEquals(30.0, savedPurchase.getValue().getTotalAmount());
        assertEquals(7, medicine.getQuantity());
        assertEquals(purchase, savedPurchase.getValue());
    }

    private PurchaseRequest request() {
        PurchaseItemRequest item = new PurchaseItemRequest();
        item.setMedicineId(2L);
        item.setQuantity(2);
        item.setPurchaseRate(15.0);
        item.setSaleRate(20.0);
        item.setBatchNo("BATCH-01");
        item.setGst(5.0);
        item.setExpiryDate("2028-12-31");

        PurchaseRequest request = new PurchaseRequest();
        request.setSupplierId(1L);
        request.setInvoiceNo("INV-01");
        request.setPurchaseDate("2026-11-20");
        request.setBatchNo("BATCH-01");
        request.setItems(List.of(item));
        return request;
    }
}
