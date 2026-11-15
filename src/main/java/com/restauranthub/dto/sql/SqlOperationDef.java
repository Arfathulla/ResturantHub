package com.restauranthub.dto.sql;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlOperationDef {
    private int number;
    private String key;
    private String name;
    private String category; // "Basic SQL Operations", "Intermediate SQL Operations", "Advanced SQL Operations"
    private String targetEntity;
    private String description;
    private String sql;
}
