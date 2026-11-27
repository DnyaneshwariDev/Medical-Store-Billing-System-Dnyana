package com.medical.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.medical.entity.Medicine;
import com.medical.repository.MedicineRepository;

@ExtendWith(MockitoExtension.class)
class MedicineServiceTest {

    @Mock
    private MedicineRepository medicineRepository;

    @InjectMocks
    private MedicineService medicineService;

    @Test
    void capsSearchResultsAndNormalizesRequest() {
        when(medicineRepository.searchMedicines(eq("A"), any(Pageable.class)))
                .thenReturn(Page.empty());

        medicineService.searchMedicines("  A  ", 0, 1000);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(medicineRepository).searchMedicines(eq("A"), pageable.capture());
        assertEquals(25, pageable.getValue().getPageSize());
    }

    @Test
    void skipsRepositorySearchForBlankQuery() {
        Page<Medicine> result = medicineService.searchMedicines("  ", 0, 20);

        assertEquals(0, result.getTotalElements());
        verify(medicineRepository, never()).searchMedicines(any(), any(Pageable.class));
    }
}
