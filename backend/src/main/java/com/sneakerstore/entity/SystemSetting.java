package com.sneakerstore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "SYSTEM_SETTINGS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSetting {

    @Id
    @Column(name = "SETTING_KEY", length = 80)
    private String settingKey;

    @Column(name = "SETTING_VALUE", nullable = false, length = 255)
    private String settingValue;

    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void touch() {
        updatedAt = LocalDateTime.now();
    }
}
