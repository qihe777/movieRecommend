package com.example.business.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class RetResult<T> {
    private boolean status;
    private T data;
}
