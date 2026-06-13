package com.libuke.evidence.api.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PageResponse<T> {

    private long total;
    private long pageNo;
    private long pageSize;
    private List<T> records;
}
