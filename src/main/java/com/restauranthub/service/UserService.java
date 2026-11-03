package com.restauranthub.service;

import com.restauranthub.dto.UserProfileDto;
import com.restauranthub.entity.Role;
import com.restauranthub.entity.User;
import com.restauranthub.entity.enums.UserStatus;
import com.restauranthub.exception.ResourceNotFoundException;
import com.restauranthub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    @Transactional(readOnly = true)
    public UserProfileDto getUserProfile(Long id) {
        User user = findById(id);
        return mapToProfileDto(user);
    }

    @Transactional
    public UserProfileDto updateProfile(Long id, UserProfileDto dto) {
        User user = findById(id);
        user.setFirstName(dto.getFirstName().trim());
        user.setLastName(dto.getLastName().trim());
        if (dto.getPhoneNumber() != null) {
            user.setPhoneNumber(dto.getPhoneNumber().trim());
        }
        User updated = userRepository.save(user);
        return mapToProfileDto(updated);
    }

    @Transactional
    public void suspendUser(Long id) {
        User user = findById(id);
        user.setStatus(UserStatus.SUSPENDED);
        userRepository.save(user);
    }

    @Transactional
    public void activateUser(Long id) {
        User user = findById(id);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<UserProfileDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToProfileDto)
                .collect(Collectors.toList());
    }

    private UserProfileDto mapToProfileDto(User user) {
        return UserProfileDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .status(user.getStatus().name())
                .emailVerified(user.isEmailVerified())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toList()))
                .createdAt(user.getCreatedAt())
                .build();
    }
}
