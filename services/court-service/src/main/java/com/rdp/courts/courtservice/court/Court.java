package com.rdp.courts.courtservice.court;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "courts")
class Court {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String name;
    private boolean isActive;
    @CreationTimestamp
    private LocalDateTime createdAt;

    // Required by JPA
    Court() {
    }

    Court(final String name, final boolean isActive) {
        this.name = name;
        this.isActive = isActive;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean getIsActive() {
        return isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }
}
