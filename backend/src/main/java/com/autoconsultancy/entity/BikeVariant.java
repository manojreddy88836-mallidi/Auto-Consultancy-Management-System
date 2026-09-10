package com.autoconsultancy.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bike_variants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BikeVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bike_model_id")
    @JsonIgnore
    private BikeModel bikeModel;

    private String variantName;
    private Integer engineCC;
    
    @Builder.Default
    private boolean active = true;
}
