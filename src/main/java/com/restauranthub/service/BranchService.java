package com.restauranthub.service;

import com.restauranthub.dto.BranchClosureDto;
import com.restauranthub.dto.BranchCreateUpdateDto;
import com.restauranthub.dto.BranchDto;
import com.restauranthub.dto.OperatingHoursDto;
import com.restauranthub.entity.OperatingHours;
import com.restauranthub.entity.RestaurantBranch;
import com.restauranthub.entity.RestaurantClosure;
import com.restauranthub.exception.BadRequestException;
import com.restauranthub.exception.ResourceNotFoundException;
import com.restauranthub.repository.OperatingHoursRepository;
import com.restauranthub.repository.RestaurantBranchRepository;
import com.restauranthub.repository.RestaurantClosureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final RestaurantBranchRepository branchRepository;
    private final OperatingHoursRepository operatingHoursRepository;
    private final RestaurantClosureRepository closureRepository;

    @Transactional(readOnly = true)
    public List<BranchDto> getAllActiveBranches() {
        return branchRepository.findAllByIsActiveTrue().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BranchDto getBranchById(Long id) {
        RestaurantBranch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RestaurantBranch", "id", id));
        return mapToDto(branch);
    }

    @Transactional(readOnly = true)
    public BranchDto getBranchByCode(String code) {
        RestaurantBranch branch = branchRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("RestaurantBranch", "code", code));
        return mapToDto(branch);
    }

    @Transactional(readOnly = true)
    public List<BranchDto> searchBranches(String query) {
        if (query == null || query.isBlank()) {
            return getAllActiveBranches();
        }
        return branchRepository.searchBranches(query.trim()).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public BranchDto createBranch(BranchCreateUpdateDto dto) {
        if (branchRepository.findByCode(dto.getCode().trim().toUpperCase()).isPresent()) {
            throw new BadRequestException("DUPLICATE_CODE", "Branch code already exists: " + dto.getCode());
        }

        RestaurantBranch branch = RestaurantBranch.builder()
                .name(dto.getName().trim())
                .code(dto.getCode().trim().toUpperCase())
                .description(dto.getDescription())
                .address(dto.getAddress().trim())
                .city(dto.getCity().trim())
                .state(dto.getState().trim())
                .postalCode(dto.getPostalCode().trim())
                .phoneNumber(dto.getPhoneNumber().trim())
                .email(dto.getEmail().trim().toLowerCase())
                .timeZone(dto.getTimeZone() != null ? dto.getTimeZone() : "UTC")
                .imageUrl(dto.getImageUrl())
                .isActive(dto.isActive())
                .build();

        RestaurantBranch saved = branchRepository.save(branch);

        if (dto.getOperatingHours() != null && !dto.getOperatingHours().isEmpty()) {
            List<OperatingHours> hoursList = dto.getOperatingHours().stream()
                    .map(h -> OperatingHours.builder()
                            .branch(saved)
                            .dayOfWeek(h.getDayOfWeek())
                            .openTime(h.getOpenTime())
                            .closeTime(h.getCloseTime())
                            .isClosed(h.isClosed())
                            .build())
                    .collect(Collectors.toList());
            operatingHoursRepository.saveAll(hoursList);
        }

        return getBranchById(saved.getId());
    }

    @Transactional
    public BranchDto updateBranch(Long id, BranchCreateUpdateDto dto) {
        RestaurantBranch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RestaurantBranch", "id", id));

        branch.setName(dto.getName().trim());
        branch.setDescription(dto.getDescription());
        branch.setAddress(dto.getAddress().trim());
        branch.setCity(dto.getCity().trim());
        branch.setState(dto.getState().trim());
        branch.setPostalCode(dto.getPostalCode().trim());
        branch.setPhoneNumber(dto.getPhoneNumber().trim());
        branch.setEmail(dto.getEmail().trim().toLowerCase());
        if (dto.getTimeZone() != null) {
            branch.setTimeZone(dto.getTimeZone());
        }
        branch.setImageUrl(dto.getImageUrl());
        branch.setActive(dto.isActive());

        branchRepository.save(branch);
        return getBranchById(id);
    }

    @Transactional
    public BranchClosureDto addClosure(Long branchId, BranchClosureDto dto) {
        RestaurantBranch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("RestaurantBranch", "id", branchId));

        if (closureRepository.existsByBranchIdAndClosureDate(branchId, dto.getClosureDate())) {
            throw new BadRequestException("DUPLICATE_CLOSURE", "Branch closure already registered for date: " + dto.getClosureDate());
        }

        RestaurantClosure closure = RestaurantClosure.builder()
                .branch(branch)
                .closureDate(dto.getClosureDate())
                .reason(dto.getReason())
                .build();

        RestaurantClosure saved = closureRepository.save(closure);
        return BranchClosureDto.builder()
                .id(saved.getId())
                .closureDate(saved.getClosureDate())
                .reason(saved.getReason())
                .build();
    }

    @Transactional(readOnly = true)
    public boolean isBranchOpenAt(Long branchId, LocalDate date, LocalTime time) {
        // 1. Check if date is an official branch closure
        if (closureRepository.existsByBranchIdAndClosureDate(branchId, date)) {
            return false;
        }

        // 2. Check scheduled operating hours
        Optional<OperatingHours> hoursOpt = operatingHoursRepository.findByBranchIdAndDayOfWeek(branchId, date.getDayOfWeek());
        if (hoursOpt.isEmpty() || hoursOpt.get().isClosed()) {
            return false;
        }

        OperatingHours hours = hoursOpt.get();
        // Standard check: openTime <= time <= closeTime
        return !time.isBefore(hours.getOpenTime()) && !time.isAfter(hours.getCloseTime());
    }

    private BranchDto mapToDto(RestaurantBranch branch) {
        List<OperatingHoursDto> hours = operatingHoursRepository.findByBranchId(branch.getId()).stream()
                .map(h -> OperatingHoursDto.builder()
                        .id(h.getId())
                        .dayOfWeek(h.getDayOfWeek())
                        .openTime(h.getOpenTime())
                        .closeTime(h.getCloseTime())
                        .isClosed(h.isClosed())
                        .build())
                .collect(Collectors.toList());

        List<BranchClosureDto> closures = closureRepository.findByBranchId(branch.getId()).stream()
                .map(c -> BranchClosureDto.builder()
                        .id(c.getId())
                        .closureDate(c.getClosureDate())
                        .reason(c.getReason())
                        .build())
                .collect(Collectors.toList());

        return BranchDto.builder()
                .id(branch.getId())
                .name(branch.getName())
                .code(branch.getCode())
                .description(branch.getDescription())
                .address(branch.getAddress())
                .city(branch.getCity())
                .state(branch.getState())
                .postalCode(branch.getPostalCode())
                .phoneNumber(branch.getPhoneNumber())
                .email(branch.getEmail())
                .timeZone(branch.getTimeZone())
                .imageUrl(branch.getImageUrl())
                .isActive(branch.isActive())
                .operatingHours(hours)
                .upcomingClosures(closures)
                .build();
    }
}
