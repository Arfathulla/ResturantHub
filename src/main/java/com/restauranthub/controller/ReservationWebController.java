package com.restauranthub.controller;

import com.restauranthub.dto.*;
import com.restauranthub.entity.User;
import com.restauranthub.entity.enums.VehicleType;
import com.restauranthub.exception.ApiException;
import com.restauranthub.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationWebController {

    private final TableReservationService tableService;
    private final RoomReservationService roomService;
    private final HallReservationService hallService;
    private final ParkingReservationService parkingService;
    private final BranchService branchService;
    private final AuthService authService;

    @GetMapping("/tables/new")
    public String showTableReservationForm(@RequestParam(value = "branchId", required = false) Long branchId, Model model) {
        TableReservationRequestDto dto = new TableReservationRequestDto();
        if (branchId != null) {
            dto.setBranchId(branchId);
        }
        model.addAttribute("tableDto", dto);
        model.addAttribute("branches", branchService.getAllActiveBranches());
        return "reservations/table-booking";
    }

    @PostMapping("/tables")
    public String submitTableReservation(@Valid @ModelAttribute("tableDto") TableReservationRequestDto dto,
                                         BindingResult bindingResult,
                                         RedirectAttributes redirectAttributes,
                                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("branches", branchService.getAllActiveBranches());
            return "reservations/table-booking";
        }

        try {
            User user = authService.getCurrentAuthenticatedUser();
            TableReservationResponseDto response = tableService.reserveTable(user, dto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Table booked successfully! Reference: " + response.getReferenceNo());
            return "redirect:/reservations/tables/" + response.getReferenceNo();
        } catch (ApiException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("branches", branchService.getAllActiveBranches());
            return "reservations/table-booking";
        }
    }

    @GetMapping("/tables/{referenceNo}")
    public String showTableBookingConfirmation(@PathVariable String referenceNo, Model model) {
        User user = authService.getCurrentAuthenticatedUser();
        TableReservationResponseDto res = tableService.getReservationByReference(referenceNo, user);
        model.addAttribute("reservation", res);
        return "reservations/table-confirmation";
    }

    @GetMapping("/rooms/new")
    public String showRoomBookingForm(@RequestParam(value = "branchId", required = false) Long branchId, Model model) {
        RoomReservationRequestDto dto = new RoomReservationRequestDto();
        if (branchId != null) {
            dto.setBranchId(branchId);
        }
        model.addAttribute("roomDto", dto);
        model.addAttribute("branches", branchService.getAllActiveBranches());
        return "reservations/room-booking";
    }

    @PostMapping("/rooms")
    public String submitRoomReservation(@Valid @ModelAttribute("roomDto") RoomReservationRequestDto dto,
                                        BindingResult bindingResult,
                                        RedirectAttributes redirectAttributes,
                                        Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("branches", branchService.getAllActiveBranches());
            return "reservations/room-booking";
        }

        try {
            User user = authService.getCurrentAuthenticatedUser();
            RoomReservationResponseDto response = roomService.reserveRoom(user, dto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Private Dining Room reserved! Reference: " + response.getReferenceNo());
            return "redirect:/reservations/rooms/" + response.getReferenceNo();
        } catch (ApiException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("branches", branchService.getAllActiveBranches());
            return "reservations/room-booking";
        }
    }

    @GetMapping("/rooms/{referenceNo}")
    public String showRoomBookingConfirmation(@PathVariable String referenceNo, Model model) {
        User user = authService.getCurrentAuthenticatedUser();
        RoomReservationResponseDto res = roomService.getReservationByReference(referenceNo, user);
        model.addAttribute("reservation", res);
        return "reservations/room-confirmation";
    }

    @GetMapping("/halls/new")
    public String showHallBookingForm(@RequestParam(value = "branchId", required = false) Long branchId, Model model) {
        HallReservationRequestDto dto = new HallReservationRequestDto();
        if (branchId != null) {
            dto.setBranchId(branchId);
        }
        model.addAttribute("hallDto", dto);
        model.addAttribute("branches", branchService.getAllActiveBranches());
        return "reservations/hall-booking";
    }

    @PostMapping("/halls")
    public String submitHallReservation(@Valid @ModelAttribute("hallDto") HallReservationRequestDto dto,
                                        BindingResult bindingResult,
                                        RedirectAttributes redirectAttributes,
                                        Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("branches", branchService.getAllActiveBranches());
            return "reservations/hall-booking";
        }

        try {
            User user = authService.getCurrentAuthenticatedUser();
            HallReservationResponseDto response = hallService.reserveHall(user, dto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Party Hall reserved! Reference: " + response.getReferenceNo());
            return "redirect:/reservations/halls/" + response.getReferenceNo();
        } catch (ApiException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("branches", branchService.getAllActiveBranches());
            return "reservations/hall-booking";
        }
    }

    @GetMapping("/halls/{referenceNo}")
    public String showHallBookingConfirmation(@PathVariable String referenceNo, Model model) {
        User user = authService.getCurrentAuthenticatedUser();
        HallReservationResponseDto res = hallService.getReservationByReference(referenceNo, user);
        model.addAttribute("reservation", res);
        return "reservations/hall-confirmation";
    }

    @GetMapping("/parking/new")
    public String showParkingBookingForm(@RequestParam(value = "branchId", required = false) Long branchId, Model model) {
        ParkingReservationRequestDto dto = new ParkingReservationRequestDto();
        if (branchId != null) {
            dto.setBranchId(branchId);
        }
        model.addAttribute("parkingDto", dto);
        model.addAttribute("branches", branchService.getAllActiveBranches());
        model.addAttribute("vehicleTypes", VehicleType.values());
        return "reservations/parking-booking";
    }

    @PostMapping("/parking")
    public String submitParkingReservation(@Valid @ModelAttribute("parkingDto") ParkingReservationRequestDto dto,
                                           BindingResult bindingResult,
                                           RedirectAttributes redirectAttributes,
                                           Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("branches", branchService.getAllActiveBranches());
            model.addAttribute("vehicleTypes", VehicleType.values());
            return "reservations/parking-booking";
        }

        try {
            User user = authService.getCurrentAuthenticatedUser();
            ParkingReservationResponseDto response = parkingService.reserveParking(user, dto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Parking slot reserved! Reference: " + response.getReferenceNo());
            return "redirect:/reservations/parking/" + response.getReferenceNo();
        } catch (ApiException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("branches", branchService.getAllActiveBranches());
            model.addAttribute("vehicleTypes", VehicleType.values());
            return "reservations/parking-booking";
        }
    }

    @GetMapping("/parking/{referenceNo}")
    public String showParkingBookingConfirmation(@PathVariable String referenceNo, Model model) {
        User user = authService.getCurrentAuthenticatedUser();
        ParkingReservationResponseDto res = parkingService.getReservationByReference(referenceNo, user);
        model.addAttribute("reservation", res);
        return "reservations/parking-confirmation";
    }

    @GetMapping("/my-bookings")
    public String showMyBookings(Model model) {
        User user = authService.getCurrentAuthenticatedUser();
        model.addAttribute("tables", tableService.getCustomerReservations(user.getId()));
        model.addAttribute("rooms", roomService.getCustomerReservations(user.getId()));
        model.addAttribute("halls", hallService.getCustomerReservations(user.getId()));
        model.addAttribute("parking", parkingService.getCustomerReservations(user.getId()));
        return "reservations/my-bookings";
    }
}
