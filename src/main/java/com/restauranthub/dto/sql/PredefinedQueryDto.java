package com.restauranthub.dto.sql;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredefinedQueryDto {

    private String id;
    private String module;
    private String name;
    private String description;
    private String sql;
    private String queryType;
}
