package com.example.fleetfocus.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PageResult<T> {
    private final List<T> items;
    private final long totalElements;
}
