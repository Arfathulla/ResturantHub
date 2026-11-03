package com.restauranthub.controller;

import com.restauranthub.dto.*;
import com.restauranthub.entity.User;
import com.restauranthub.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservations Engine", description = "Multi-resource booking endpoints for dining tables, private rooms, party halls, and parking")
public class ReservationRestController {

    private final TableReservationService tableService;
    private final RoomReservationService roomService;
    private final HallReservationService hallService;
    private final ParkingReservationService parkingService;
    private final AuthService authService;

    // --- Table Reservations ---
    @PostMapping("/tables")
    @Operation(summary = "Reserve a dining table with concurrency-safe pessimistic locking")
    public ResponseEntity<ApiResponse<TableReservationResponseDto>> reserveTable(
            @Valid @RequestBody TableReservationRequestDto request) {
        User user = authService.getCurrentAuthenticatedUser();
        TableReservationResponseDto response = tableService.reserveTable(user, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Table reservation created successfully", response));
    }

    @GetMapping("/tables/{referenceNo}")
    @Operation(summary = "Get table reservation details by reference number")
    public ResponseEntity<ApiResponse<TableReservationResponseDto>> getTableReservation(
            @PathVariable String referenceNo) {
        User user = authService.getCurrentAuthenticatedUser();
        TableReservationResponseDto response = tableService.getReservationByReference(referenceNo, user);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/tables/{referenceNo}/cancel")
    @Operation(summary = "Cancel an existing table reservation honoring the 2-hour cancellation policy")
    public ResponseEntity<ApiResponse<TableReservationResponseDto>> cancelTableReservation(
            @PathVariable String referenceNo,
            @RequestParam(value = "reason", defaultValue = "Customer requested cancellation") String reason) {
        User user = authService.getCurrentAuthenticatedUser();
        TableReservationResponseDto response = tableService.cancelReservation(referenceNo, user, reason);
        return ResponseEntity.ok(ApiResponse.success("Table reservation cancelled successfully", response));
    }

    // --- Private Room Reservations ---
    @PostMapping("/rooms")
    @Operation(summary = "Reserve a private dining room with server-calculated duration charges")
    public ResponseEntity<ApiResponse<RoomReservationResponseDto>> reserveRoom(
            @Valid @RequestBody RoomReservationRequestDto request) {
        User user = authService.getCurrentAuthenticatedUser();
        RoomReservationResponseDto response = roomService.reserveRoom(user, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Private room reservation created successfully", response));
    }

    @GetMapping("/rooms/{referenceNo}")
    @Operation(summary = "Get private dining room reservation details")
    public ResponseEntity<ApiResponse<RoomReservationResponseDto>> getRoomReservation(
            @PathVariable String referenceNo) {
        User user = authService.getCurrentAuthenticatedUser();
        RoomReservationResponseDto response = roomService.getReservationByReference(referenceNo, user);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/rooms/{referenceNo}/cancel")
    @Operation(summary = "Cancel private room reservation honoring 12-hour policy")
    public ResponseEntity<ApiResponse<RoomReservationResponseDto>> cancelRoomReservation(
            @PathVariable String referenceNo) {
        User user = authService.getCurrentAuthenticatedUser();
        RoomReservationResponseDto response = roomService.cancelReservation(referenceNo, user);
        return ResponseEntity.ok(ApiResponse.success("Room reservation cancelled successfully", response));
    }

    // --- Party Hall Reservations ---
    @PostMapping("/halls")
    @Operation(summary = "Reserve a party hall with setup buffer and catering package pricing")
    public ResponseEntity<ApiResponse<HallReservationResponseDto>> reserveHall(
            @Valid @RequestBody HallReservationRequestDto request) {
        User user = authService.getCurrentAuthenticatedUser();
        HallReservationResponseDto response = hallService.reserveHall(user, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Party hall reservation created successfully", response));
    }

    @GetMapping("/halls/{referenceNo}")
    @Operation(summary = "Get party hall reservation details")
    public ResponseEntity<ApiResponse<HallReservationResponseDto>> getHallReservation(
            @PathVariable String referenceNo) {
        User user = authService.getCurrentAuthenticatedUser();
        HallReservationResponseDto response = hallService.getReservationByReference(referenceNo, user);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/halls/{referenceNo}/cancel")
    @Operation(summary = "Cancel party hall reservation honoring 48-hour policy")
    public ResponseEntity<ApiResponse<HallReservationResponseDto>> cancelHallReservation(
            @PathVariable String referenceNo) {
        User user = authService.getCurrentAuthenticatedUser();
        HallReservationResponseDto response = hallService.cancelReservation(referenceNo, user);
        return ResponseEntity.ok(ApiResponse.success("Party hall reservation cancelled successfully", response));
    }

    // --- Parking Reservations ---
    @PostMapping("/parking")
    @Operation(summary = "Reserve a vehicle parking slot with type compatibility checks")
    public ResponseEntity<ApiResponse<ParkingReservationResponseDto>> reserveParking(
            @Valid @RequestBody ParkingReservationRequestDto request) {
        User user = authService.getCurrentAuthenticatedUser();
        ParkingReservationResponseDto response = parkingService.reserveParking(user, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Parking slot reserved successfully", response));
    }

    @GetMapping("/parking/{referenceNo}")
    @Operation(summary = "Get parking reservation details")
    public ResponseEntity<ApiResponse<ParkingReservationResponseDto>> getParkingReservation(
            @PathVariable String referenceNo) {
        User user = authService.getCurrentAuthenticatedUser();
        ParkingReservationResponseDto response = parkingService.getReservationByReference(referenceNo, user);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/parking/{referenceNo}/cancel")
    @Operation(summary = "Cancel parking reservation")
    public ResponseEntity<ApiResponse<ParkingReservationResponseDto>> cancelParkingReservation(
            @PathVariable String referenceNo) {
        User user = authService.getCurrentAuthenticatedUser();
        ParkingReservationResponseDto response = parkingService.cancelReservation(referenceNo, user);
        return ResponseEntity.ok(ApiResponse.success("Parking reservation cancelled successfully", response));
    }

    // --- Consolidated Customer Bookings ---
    @GetMapping("/me")
    @Operation(summary = "Get all active and past bookings across tables, rooms, halls, and parking for current user")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyReservations() {
        User user = authService.getCurrentAuthenticatedUser();
        Map<String, Object> allBookings = new HashMap<>();
        allBookings.put("tables", tableService.getCustomerReservations(user.getId()));
        allBookings.put("rooms", roomService.getCustomerReservations(user.getId()));
        allBookings.put("halls", hallService.getCustomerReservations(user.getId()));
        allBookings.put("parking", parkingService.getCustomerReservations(user.getId()));
        return ResponseEntity.ok(ApiResponse.success(allBookings));
    }
}
