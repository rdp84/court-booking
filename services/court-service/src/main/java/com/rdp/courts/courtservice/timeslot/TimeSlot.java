package com.rdp.courts.courtservice.timeslot;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import com.rdp.courts.courtservice.court.Court;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "time_slots")
class TimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "court_id")
    private Court court;

    private LocalTime slotStart;

    @Generated(event = { EventType.INSERT, EventType.UPDATE })
    @Column(insertable = false, updatable = false)
    private LocalTime slotEnd;

    @CreationTimestamp
    private LocalDateTime createdAt;

    // Required by JPA
    TimeSlot() {
    }

    TimeSlot(Court court, LocalTime slotStart) {
        this.court = court;
        this.slotStart = slotStart;
    }

    TimeSlot(Court court, LocalTime slotStart, LocalTime slotEnd) {
        this.court = court;
        this.slotStart = slotStart;
        this.slotEnd = slotEnd;
    }

    public UUID getId() {
        return id;
    }

    public Court getCourt() {
        return court;
    }

    public LocalTime getSlotStart() {
        return slotStart;
    }

    public LocalTime getSlotEnd() {
        return slotEnd;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

}
