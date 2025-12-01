package com.construmedicis.buildtracking.attendance.models;

import java.time.LocalDateTime;

import com.construmedicis.buildtracking.participation.models.Participation;

import lombok.*;
import jakarta.persistence.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "attendance")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "attendance_id_seq")
    @SequenceGenerator(name = "attendance_id_seq", sequenceName = "attendance_id_seq", allocationSize = 1)
    @Column(name = "attendance_id")
    private Long id;
    @Column(name = "attendance_date")
    private LocalDateTime attendanceDate;
    @Column(name = "attendance_present")
    private Boolean present;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "participation_id")
    private Participation participation;

}
