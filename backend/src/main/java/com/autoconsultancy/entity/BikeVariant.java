package com.autoconsultancy.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "bike_variants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BikeVariant {

    @Id
    private Long id;

    @DBRef
    @JsonIgnore
    private BikeModel bikeModel;

    private String variantName;
    private Integer engineCC;
    
    @Builder.Default
    private boolean active = true;
}
