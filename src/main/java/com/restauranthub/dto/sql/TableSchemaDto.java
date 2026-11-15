package com.restauranthub.dto.sql;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableSchemaDto {

    private String tableName;
    private long rowCount;

    @Builder.Default
    private List<ColumnDetailDto> columns = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnDetailDto {
        private String name;
        private String type;
        private boolean nullable;
        private boolean primaryKey;
        private String defaultValue;
    }
}
