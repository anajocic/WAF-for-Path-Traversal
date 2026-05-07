package com.example.websecurity.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Long id;

    private String title;
    private String director;
    private Integer year;
    private String description;

    @Column(name = "running_time")
    private Integer runningTime;

    @Column(name = "imdb_score")
    private Integer imdbScore;

    @Column(nullable = false)
    private ZonedDateTime created;

    @Column(nullable = false)
    private ZonedDateTime updated;

    @PrePersist
    @PreUpdate
    public void setDates() {
        ZonedDateTime now = ZonedDateTime.now();
        if (getCreated() == null) {
            setCreated(now);
        }
        setUpdated(now);
    }

}
