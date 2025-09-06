package com.tbm.admin.model.view.base;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown=true) // ignore unknown properties when deserialize
@JsonInclude(JsonInclude.Include.NON_NULL) // not null only
@RequiredArgsConstructor
public class DataTableView {

    private final int draw;
    private final long recordsTotal;
    private final long recordsFiltered;
    private final Map<String, Object> data;
}