package com.restauranthub.controller;

import com.restauranthub.dto.OrderResponseDto;
import com.restauranthub.dto.UpdateOrderStatusRequestDto;
import com.restauranthub.entity.User;
import com.restauranthub.entity.enums.OrderStatus;
import com.restauranthub.service.AuthService;
import com.restauranthub.service.BranchService;
import com.restauranthub.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/kitchen")
@PreAuthorize("hasAnyAuthority('ROLE_KITCHEN', 'ROLE_MANAGER', 'ROLE_ADMIN')")
@RequiredArgsConstructor
public class KitchenWebController {

    private final OrderService orderService;
    private final BranchService branchService;
    private final AuthService authService;

    @GetMapping("/queue")
    public String showKitchenQueue(@RequestParam(value = "branchId", defaultValue = "1") Long branchId, Model model) {
        List<OrderResponseDto> liveOrders = orderService.getBranchLiveOrders(branchId);
        model.addAttribute("liveOrders", liveOrders);
        model.addAttribute("branches", branchService.getAllActiveBranches());
        model.addAttribute("selectedBranchId", branchId);
        return "kitchen/live-queue";
    }

    @PostMapping("/orders/{orderNumber}/status")
    public String updateKitchenStatus(@PathVariable String orderNumber,
                                      @RequestParam("newStatus") OrderStatus newStatus,
                                      @RequestParam(value = "branchId", defaultValue = "1") Long branchId,
                                      RedirectAttributes redirectAttributes) {
        User chef = authService.getCurrentAuthenticatedUser();
        UpdateOrderStatusRequestDto dto = new UpdateOrderStatusRequestDto(newStatus, "Kitchen workflow update by chef " + chef.getFullName());
        orderService.updateOrderStatus(orderNumber, dto, chef);
        redirectAttributes.addFlashAttribute("successMessage", "Order " + orderNumber + " moved to " + newStatus);
        return "redirect:/kitchen/queue?branchId=" + branchId;
    }
}
