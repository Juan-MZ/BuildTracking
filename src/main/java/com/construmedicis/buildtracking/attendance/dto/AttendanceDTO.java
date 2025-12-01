package com.construmedicis.buildtracking.attendance.dto;

import java.time.LocalDateTime;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AttendanceDTO {
    private Long id;
    private LocalDateTime attendanceDate;
    private Boolean present;
    private Long participationId;
}
