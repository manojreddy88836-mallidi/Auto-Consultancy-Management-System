package com.autoconsultancy.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "manufacturing_years")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManufacturingYear {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bike_model_id")
    @JsonIgnore
    private BikeModel bikeModel;

    @Column(nullable = false)
    private Integer year;

    @Builder.Default
    private boolean active = true;
}
