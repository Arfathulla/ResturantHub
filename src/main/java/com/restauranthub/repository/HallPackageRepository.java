package com.restauranthub.repository;

import com.restauranthub.entity.HallPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HallPackageRepository extends JpaRepository<HallPackage, Long> {

    List<HallPackage> findByHallIdAndIsActiveTrue(Long hallId);
}
