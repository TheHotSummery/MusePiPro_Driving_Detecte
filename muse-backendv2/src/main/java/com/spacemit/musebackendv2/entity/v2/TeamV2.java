package com.spacemit.musebackendv2.entity.v2;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;

/**
 * V2版本车队实体类
 */
@Entity
@Table(name = "teams")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamV2 implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "team_id", unique = true, nullable = false, length = 50)
    private String teamId;

    @Column(name = "team_name", nullable = false, length = 100)
    private String teamName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    @Column(name = "deleted_at")
    private Long deletedAt;

    @PrePersist
    protected void onCreate() {
        long now = System.currentTimeMillis();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = System.currentTimeMillis();
    }
}
















