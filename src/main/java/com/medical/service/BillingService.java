package com.medical.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

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

import jakarta.transaction.Transactional;

@Service
@Transactional
public class BillingService {

    private final CustomerRepository customerRepository;
    private final MedicineRepository medicineRepository;
    private final BillRepository billRepository;
    private final BillItemRepository billItemRepository;

    public BillingService(
            CustomerRepository customerRepository,
            MedicineRepository medicineRepository,
            BillRepository billRepository,
            BillItemRepository billItemRepository) {

        this.customerRepository = customerRepository;
        this.medicineRepository = medicineRepository;
        this.billRepository = billRepository;
        this.billItemRepository = billItemRepository;
    }

    public Bill generateBill(BillRequest request) {
    	Customer customer;

    	if(request.getCustomerId() != null){

    	    customer = customerRepository
    	            .findById(request.getCustomerId())
    	            .orElseThrow();

    	}else{

    	    customer = customerRepository
    	            .findByCustomerName(request.getCustomerName());

    	    if(customer == null){

    	        customer = new Customer();

    	        customer.setCustomerName(request.getCustomerName());
    	        customer.setMobileNo("");
    	        customer.setAddress("");
    	        customer.setBalance(0.0);

    	        customer = customerRepository.save(customer);
    	    }
    	}
         
        double subTotal = 0;

        Bill bill = new Bill();

        bill.setCustomer(customer);
        bill.setBillDate(LocalDateTime.now());

        bill.setDoctorName(
                request.getDoctorName());

        bill.setBillType(
                request.getBillType());

        Bill savedBill =
                billRepository.save(bill);

        if (request.getMedicines() == null
                || request.getMedicines().isEmpty()) {
            throw new IllegalArgumentException(
                    "A bill must contain at least one item");
        }

        for (BillMedicineDto dto : request.getMedicines()) {
            if (dto.getQuantity() == null || dto.getQuantity() <= 0) {
                throw new IllegalArgumentException(
                        "Item quantity must be greater than zero");
            }

            Medicine medicine = dto.getMedicineId() == null
                    ? null
                    : medicineRepository.findById(dto.getMedicineId())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Selected medicine was not found"));

            if (medicine != null
                    && (medicine.getQuantity() == null
                            || medicine.getQuantity() < dto.getQuantity())) {
                throw new IllegalArgumentException(
                        "Stock not available for "
                                + medicine.getMedicineName());
            }

            String medicineName = dto.getMedicineName() != null
                    ? dto.getMedicineName().trim()
                    : medicine == null ? null : medicine.getMedicineName();
            if (medicineName == null || medicineName.isBlank()) {
                throw new IllegalArgumentException(
                        "Every bill item must have a medicine name");
            }

            Double unitPrice = dto.getUnitPrice() != null
                    ? dto.getUnitPrice()
                    : medicine == null ? null : medicine.getMrp();
            if (unitPrice == null || !Double.isFinite(unitPrice) || unitPrice < 0) {
                throw new IllegalArgumentException(
                        "Every bill item must have a valid rate");
            }

            double amount = unitPrice * dto.getQuantity();

            BillItem item =
                    new BillItem();

            item.setBill(savedBill);
            item.setMedicine(medicine);
            item.setQuantity(dto.getQuantity());
            item.setUnitPrice(unitPrice);
            item.setAmount(amount);
            item.setMedicineName(medicineName);
            item.setCompanyName(valueOrFallback(
                    dto.getCompanyName(),
                    medicine == null ? null : medicine.getCompanyName()));
            item.setBatchNo(valueOrFallback(
                    dto.getBatchNo(),
                    medicine == null ? null : medicine.getBatchNo()));
            item.setScheduleType(valueOrFallback(
                    dto.getScheduleType(),
                    medicine == null ? null : medicine.getScheduleType()));
            item.setUnit(valueOrFallback(
                    dto.getUnit(),
                    medicine == null ? null : medicine.getUnit()));
            item.setMrp(unitPrice);
            item.setGstPercent(dto.getGstPercent() != null
                    ? dto.getGstPercent()
                    : medicine == null ? null : medicine.getGstPercent());
            item.setContents(valueOrFallback(
                    dto.getContents(),
                    medicine == null ? null : medicine.getContents()));
            item.setSubstitutes(valueOrFallback(
                    dto.getSubstitutes(),
                    medicine == null ? null : medicine.getSymptoms()));
            item.setExpiryDate(medicine == null ? null : medicine.getExpiryDate());

            billItemRepository.save(item);

            if (medicine != null) {
                medicine.setQuantity(
                        medicine.getQuantity() - dto.getQuantity());
                medicineRepository.save(medicine);
            }

            subTotal += amount;
        }

        // GST 18%
        double gst = subTotal * 0.18;

        // Discount
        double discount =
                request.getDiscount() == null
                ? 0
                : request.getDiscount();

        double total =
                subTotal - discount;

        double paid =
                request.getPaidAmount() == null
                ? 0
                : request.getPaidAmount();

        double balance =
                total - paid;

        savedBill.setSubTotal(subTotal);
        savedBill.setGst(gst);
        savedBill.setDiscount(discount);
        savedBill.setTotalAmount(total);

        savedBill.setPaidAmount(paid);
        savedBill.setBalanceAmount(balance);
        double oldBalance = customer.getBalance() == null ? 0 : customer.getBalance();

        double netAmount = bill.getTotalAmount();

        double paidAmount = bill.getPaidAmount();

        double remainingAmount = netAmount - paidAmount;

        customer.setBalance(
            oldBalance + remainingAmount
        );

        customerRepository.save(customer);
        return billRepository.save(savedBill);
    }

    public List<Bill> getAllBills() {
        return billRepository.findAll();
    }
   
    public List<Bill> searchBills(String customer, String type) {
        return billRepository.searchBills(customer, type);
    }

    private String valueOrFallback(String preferred, String fallback) {
        return preferred == null
                ? fallback
                : preferred;
    }
}