package com.medical.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.medical.dto.BillMedicineDto;
import com.medical.dto.BillRequest;
import com.medical.entity.Bill;
import com.medical.entity.BillItem;
import com.medical.entity.Customer;
import com.medical.entity.Medicine;
import com.medical.repository.BillItemRepository;
import com.medical.repository.BillRepository;
import com.medical.repository.CustomerRepository;
import com.medical.repository.MedicineRepository;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private BillRepository billRepository;

    @Mock
    private BillItemRepository billItemRepository;

    @InjectMocks
    private BillingService billingService;

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setBalance(0.0);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(billRepository.save(any(Bill.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(billItemRepository.save(any(BillItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void savesManualBillItemWithoutChangingInventory() {
        BillMedicineDto item = new BillMedicineDto();
        item.setMedicineName("Custom Medicine");
        item.setCompanyName("Example Labs");
        item.setBatchNo("CUSTOM-01");
        item.setScheduleType("OTC");
        item.setUnit("box");
        item.setContents("Active ingredient");
        item.setSubstitutes("Alternative");
        item.setGstPercent(5.0);
        item.setQuantity(2);
        item.setUnitPrice(12.5);

        Bill bill = billingService.generateBill(requestWith(item));

        ArgumentCaptor<BillItem> savedItem = ArgumentCaptor.forClass(BillItem.class);
        verify(billItemRepository).save(savedItem.capture());
        assertNull(savedItem.getValue().getMedicine());
        assertEquals("Custom Medicine", savedItem.getValue().getMedicineName());
        assertEquals("Example Labs", savedItem.getValue().getCompanyName());
        assertEquals("Active ingredient", savedItem.getValue().getContents());
        assertEquals("Alternative", savedItem.getValue().getSubstitutes());
        assertEquals(25.0, savedItem.getValue().getAmount(), 0.001);
        assertEquals(25.0, bill.getSubTotal(), 0.001);
        verify(medicineRepository, never()).save(any(Medicine.class));
    }

    @Test
    void savesEditedInventoryItemAndDeductsItsStock() {
        Medicine medicine = new Medicine();
        medicine.setMedicineName("Original Medicine");
        medicine.setCompanyName("Original Manufacturer");
        medicine.setBatchNo("BATCH-01");
        medicine.setScheduleType("OTC");
        medicine.setUnit("strip");
        medicine.setMrp(20.0);
        medicine.setGstPercent(12.0);
        medicine.setQuantity(10);
        when(medicineRepository.findById(9L)).thenReturn(Optional.of(medicine));
        when(medicineRepository.save(any(Medicine.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BillMedicineDto item = new BillMedicineDto();
        item.setMedicineId(9L);
        item.setMedicineName("Edited Bill Description");
        item.setCompanyName("Edited Manufacturer");
        item.setBatchNo("EDITED-BATCH");
        item.setScheduleType("H");
        item.setUnit("pack");
        item.setQuantity(3);
        item.setUnitPrice(12.5);
        item.setGstPercent(5.0);

        Bill bill = billingService.generateBill(requestWith(item));

        ArgumentCaptor<BillItem> savedItem = ArgumentCaptor.forClass(BillItem.class);
        verify(billItemRepository).save(savedItem.capture());
        assertEquals(medicine, savedItem.getValue().getMedicine());
        assertEquals("Edited Bill Description", savedItem.getValue().getMedicineName());
        assertEquals("Edited Manufacturer", savedItem.getValue().getCompanyName());
        assertEquals(37.5, savedItem.getValue().getAmount(), 0.001);
        assertEquals(7, medicine.getQuantity());
        assertEquals(37.5, bill.getSubTotal(), 0.001);
    }

    private BillRequest requestWith(BillMedicineDto item) {
        BillRequest request = new BillRequest();
        request.setCustomerId(1L);
        request.setBillType("Cash");
        request.setDiscount(0.0);
        request.setPaidAmount(0.0);
        request.setMedicines(java.util.List.of(item));
        return request;
    }
}
