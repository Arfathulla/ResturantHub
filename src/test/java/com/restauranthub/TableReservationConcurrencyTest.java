package com.restauranthub;

import com.restauranthub.dto.TableReservationRequestDto;
import com.restauranthub.entity.DiningTable;
import com.restauranthub.entity.RestaurantBranch;
import com.restauranthub.entity.TableReservation;
import com.restauranthub.entity.User;
import com.restauranthub.entity.enums.ReservationStatus;
import com.restauranthub.exception.BookingConflictException;
import com.restauranthub.repository.DiningTableRepository;
import com.restauranthub.repository.RestaurantBranchRepository;
import com.restauranthub.repository.TableReservationRepository;
import com.restauranthub.service.BranchService;
import com.restauranthub.service.TableReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TableReservationConcurrencyTest {

    @Mock
    private TableReservationRepository reservationRepository;

    @Mock
    private DiningTableRepository tableRepository;

    @Mock
    private RestaurantBranchRepository branchRepository;

    @Mock
    private BranchService branchService;

    @InjectMocks
    private TableReservationService tableReservationService;

    private User customer;
    private RestaurantBranch branch;
    private DiningTable table;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tableReservationService, "holdDurationMinutes", 15);
        ReflectionTestUtils.setField(tableReservationService, "bufferMinutes", 15);

        customer = User.builder()
                .id(1L)
                .email("test.customer@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        branch = RestaurantBranch.builder()
                .id(1L)
                .name("Downtown Flagship")
                .build();

        table = DiningTable.builder()
                .id(101L)
                .tableNumber("T-01")
                .seatingCapacity(4)
                .branch(branch)
                .isActive(true)
                .depositAmount(BigDecimal.ZERO)
                .build();
    }

    @Test
    @DisplayName("Concurrency/Conflict: Rejects booking when overlapping reservation already exists")
    void testConflictingBookingThrowsBookingConflictException() {
        TableReservationRequestDto request = new TableReservationRequestDto();
        request.setBranchId(1L);
        request.setTableId(101L);
        request.setReservationDate(LocalDate.now().plusDays(2));
        request.setStartTime(LocalTime.of(19, 0));
        request.setEndTime(LocalTime.of(21, 0));
        request.setGuestCount(4);

        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(branchService.isBranchOpenAt(eq(1L), any(LocalDate.class), any(LocalTime.class))).thenReturn(true);
        when(tableRepository.findByIdWithLock(101L)).thenReturn(Optional.of(table));

        TableReservation existingReservation = TableReservation.builder()
                .id(999L)
                .table(table)
                .reservationDate(request.getReservationDate())
                .startTime(LocalTime.of(19, 30))
                .endTime(LocalTime.of(21, 30))
                .status(ReservationStatus.CONFIRMED)
                .build();

        when(reservationRepository.findConflictingReservations(
                eq(101L), eq(request.getReservationDate()), any(LocalTime.class), any(LocalTime.class), any(Instant.class)
        )).thenReturn(List.of(existingReservation));

        BookingConflictException exception = assertThrows(BookingConflictException.class, () ->
                tableReservationService.reserveTable(customer, request)
        );

        assertTrue(exception.getMessage().contains("The requested Table") || exception.getMessage().contains("already reserved or held"));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reservation Success: Allows booking when no overlapping intervals exist")
    void testSuccessfulBookingWhenNoConflict() {
        TableReservationRequestDto request = new TableReservationRequestDto();
        request.setBranchId(1L);
        request.setTableId(101L);
        request.setReservationDate(LocalDate.now().plusDays(2));
        request.setStartTime(LocalTime.of(18, 0));
        request.setEndTime(LocalTime.of(20, 0));
        request.setGuestCount(4);

        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(branchService.isBranchOpenAt(eq(1L), any(LocalDate.class), any(LocalTime.class))).thenReturn(true);
        when(tableRepository.findByIdWithLock(101L)).thenReturn(Optional.of(table));
        when(reservationRepository.findConflictingReservations(
                eq(101L), eq(request.getReservationDate()), any(LocalTime.class), any(LocalTime.class), any(Instant.class)
        )).thenReturn(Collections.emptyList());

        when(reservationRepository.save(any(TableReservation.class))).thenAnswer(invocation -> {
            TableReservation tr = invocation.getArgument(0);
            tr.setId(501L);
            return tr;
        });

        var response = tableReservationService.reserveTable(customer, request);

        assertNotNull(response);
        assertEquals(ReservationStatus.CONFIRMED, response.getStatus());
        assertEquals("T-01", response.getTableNumber());
        verify(reservationRepository, times(1)).save(any(TableReservation.class));
    }
}
