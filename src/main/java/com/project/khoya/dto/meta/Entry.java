package com.project.khoya.dto.meta;

import lombok.Data;

import java.util.List;

@Data
public class Entry {
    private String id;
    private Long time;
    private List<Change> changes;
}
