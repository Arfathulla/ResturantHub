package com.restauranthub.controller;

import com.restauranthub.dto.ChangePasswordRequestDto;
import com.restauranthub.dto.RegisterRequestDto;
import com.restauranthub.dto.ResetPasswordRequestDto;
import com.restauranthub.dto.UserProfileDto;
import com.restauranthub.entity.User;
import com.restauranthub.exception.ApiException;
import com.restauranthub.service.AuthService;
import com.restauranthub.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthWebController {

    private final AuthService authService;
    private final UserService userService;

    @GetMapping("/login")
    public String showLoginPage(Model model) {
        return "login";
    }

    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        if (!model.containsAttribute("registerDto")) {
            model.addAttribute("registerDto", new RegisterRequestDto());
        }
        return "register";
    }

    @PostMapping("/register")
    public String handleRegistration(@Valid @ModelAttribute("registerDto") RegisterRequestDto dto,
                                     BindingResult bindingResult,
                                     RedirectAttributes redirectAttributes,
                                     Model model) {
        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            authService.register(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Account created successfully! Please log in.");
            return "redirect:/login";
        } catch (ApiException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "register";
        }
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam("email") String email,
                                       RedirectAttributes redirectAttributes) {
        String token = authService.initiatePasswordReset(email);
        if (token != null) {
            redirectAttributes.addFlashAttribute("resetTokenNotice", "DEMO MODE: Generated Reset Token is: " + token);
        }
        redirectAttributes.addFlashAttribute("infoMessage", "If the email is associated with an account, a reset token has been issued.");
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordPage(@RequestParam(value = "token", required = false) String token, Model model) {
        ResetPasswordRequestDto dto = new ResetPasswordRequestDto();
        if (token != null) {
            dto.setToken(token);
        }
        model.addAttribute("resetDto", dto);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(@Valid @ModelAttribute("resetDto") ResetPasswordRequestDto dto,
                                      BindingResult bindingResult,
                                      RedirectAttributes redirectAttributes,
                                      Model model) {
        if (bindingResult.hasErrors()) {
            return "reset-password";
        }

        try {
            authService.resetPassword(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Password updated successfully. You can now log in.");
            return "redirect:/login";
        } catch (ApiException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "reset-password";
        }
    }

    @GetMapping("/profile")
    public String showProfilePage(Model model) {
        User user = authService.getCurrentAuthenticatedUser();
        UserProfileDto profile = userService.getUserProfile(user.getId());
        model.addAttribute("user", user);
        model.addAttribute("profile", profile);
        model.addAttribute("changePasswordDto", new ChangePasswordRequestDto());
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute("profile") UserProfileDto profileDto,
                                RedirectAttributes redirectAttributes) {
        User user = authService.getCurrentAuthenticatedUser();
        userService.updateProfile(user.getId(), profileDto);
        redirectAttributes.addFlashAttribute("successMessage", "Profile details updated successfully.");
        return "redirect:/profile";
    }

    @PostMapping("/profile/change-password")
    public String changePassword(@Valid @ModelAttribute("changePasswordDto") ChangePasswordRequestDto dto,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes) {
        User user = authService.getCurrentAuthenticatedUser();
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Password did not meet complexity requirements.");
            return "redirect:/profile";
        }

        try {
            authService.changePassword(user.getId(), dto);
            redirectAttributes.addFlashAttribute("successMessage", "Password changed successfully.");
        } catch (ApiException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/profile";
    }
}
