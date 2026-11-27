package com.medical.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.medical.repository.BillItemRepository;
import com.medical.repository.BillRepository;
import com.medical.repository.CustomerRepository;
import com.medical.repository.MedicineRepository;
import com.medical.repository.PurchaseRepository;
import com.medical.repository.SupplierRepository;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin("*")
public class DashboardController {
	private static final int ALERT_LIMIT = 20;

	@Autowired private BillRepository billRepo;
	@Autowired private BillItemRepository billItemRepo;
	@Autowired private PurchaseRepository purchaseRepo;
	@Autowired private MedicineRepository medicineRepo;
	@Autowired private SupplierRepository supplierRepository;
	@Autowired private CustomerRepository customerRepository;

	@GetMapping("/inventory")
	public Map<String, Integer> inventoryGraph() {
		Object[] counts = medicineRepo.getInventoryCounts();
		Map<String, Integer> map = new HashMap<>();
		map.put("available", ((Number) counts[3]).intValue());
		map.put("lowStock", ((Number) counts[2]).intValue());
		map.put("outOfStock", ((Number) counts[1]).intValue());
		return map;
	}

	@GetMapping("/alerts")
	public Map<String, Object> getAlerts() {
		LocalDate today = LocalDate.now();
		PageRequest firstPage = PageRequest.of(0, ALERT_LIMIT);
		Map<String, Object> map = new HashMap<>();

		map.put("lowStock",
				medicineRepo.findByQuantityLessThanOrderByQuantityAscIdAsc(10, firstPage));
		map.put("expiring",
				medicineRepo.findByExpiryDateBetweenOrderByExpiryDateAscIdAsc(
						today, today.plusDays(30), firstPage));
		return map;
	}

	@GetMapping("/stast")
	public Map<String, Object> getDashboard() {
		Map<String, Object> dashboard = new HashMap<>();
		LocalDate today = LocalDate.now();
		LocalDateTime start = today.atStartOfDay();
		LocalDateTime end = today.plusDays(1).atStartOfDay();

		dashboard.put("totalMedicines", medicineRepo.count());
		dashboard.put("totalCustomers", customerRepository.count());
		dashboard.put("outOfStock", medicineRepo.countByQuantityLessThan(1));
		dashboard.put("totalSuppliers", supplierRepository.count());
		dashboard.put("totalBills",
				billRepo.countByBillDateGreaterThanEqualAndBillDateLessThan(start, end));

		double todaySales = billRepo.sumTotalAmountByBillDateBetween(start, end);
		double todayPurchase = purchaseRepo.sumTotalAmountByPurchaseDate(today);
		double todayCost = billItemRepo.sumCostByBillDateBetween(start, end);
		dashboard.put("todaySales", todaySales);
		dashboard.put("todayPurchase", todayPurchase);
		dashboard.put("todayProfit", (int) (todaySales - todayCost));
		return dashboard;
	}

	@GetMapping("/sales")
	public List<Double> salesGraph() {
		return monthlyTotals(billRepo.sumTotalAmountByMonth());
	}

	@GetMapping("/purchase")
	public List<Double> purchaseGraph() {
		return monthlyTotals(purchaseRepo.sumTotalAmountByMonth());
	}

	@GetMapping("/top-medicines")
	public Map<String, Object> topMedicines() {
		List<String> names = new ArrayList<>();
		List<Integer> quantities = new ArrayList<>();

		billItemRepo.findTopSellingMedicines(PageRequest.of(0, 5))
				.forEach(medicine -> {
					names.add(medicine.getMedicineName());
					quantities.add(medicine.getSaleQuantity().intValue());
				});

		Map<String, Object> map = new HashMap<>();
		map.put("labels", names);
		map.put("data", quantities);
		return map;
	}

	@GetMapping("/recent-purchases")
	public List<Map<String, Object>> recentPurchases() {
		List<Map<String, Object>> purchases = new ArrayList<>();
		purchaseRepo.findRecentPurchases(PageRequest.of(0, 5)).forEach(purchase -> {
			Map<String, Object> row = new HashMap<>();
			row.put("invoiceNo", purchase.getInvoiceNo());
			row.put("supplierName", purchase.getSupplierName());
			row.put("purchaseDate", purchase.getPurchaseDate());
			row.put("totalAmount", purchase.getTotalAmount());
			purchases.add(row);
		});
		return purchases;
	}

	@GetMapping("/recent-bills")
	public List<Map<String, Object>> recentBills() {
		List<Map<String, Object>> bills = new ArrayList<>();
		billRepo.findRecentBills(PageRequest.of(0, 5)).forEach(bill -> {
			Map<String, Object> row = new HashMap<>();
			row.put("id", bill.getId());
			row.put("customerName", bill.getCustomerName());
			row.put("billDate", bill.getBillDate());
			row.put("totalAmount", bill.getTotalAmount());
			bills.add(row);
		});
		return bills;
	}

	private List<Double> monthlyTotals(List<Object[]> aggregates) {
		double[] months = new double[12];
		for (Object[] aggregate : aggregates) {
			int monthIndex = ((Number) aggregate[0]).intValue() - 1;
			months[monthIndex] = ((Number) aggregate[1]).doubleValue();
		}

		List<Double> result = new ArrayList<>(months.length);
		for (double month : months) {
			result.add(month);
		}
		return result;
	}
}
