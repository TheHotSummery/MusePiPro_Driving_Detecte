package com.spacemit.musebackend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataAck {
    private String messageId;
    private boolean success;
    private String message;
    private List<String> failedIds;
}