package com.spacemit.musebackend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HeartbeatResponse {
    private String serverTime;
    private String status;
    private String message;
}