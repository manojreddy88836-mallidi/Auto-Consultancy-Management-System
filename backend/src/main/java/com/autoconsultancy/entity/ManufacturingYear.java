package com.autoconsultancy.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "manufacturing_years")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManufacturingYear {

    @Id
    private Long id;

    @DBRef
    @JsonIgnore
    private BikeModel bikeModel;

    private Integer year;

    @Builder.Default
    private boolean active = true;
}
