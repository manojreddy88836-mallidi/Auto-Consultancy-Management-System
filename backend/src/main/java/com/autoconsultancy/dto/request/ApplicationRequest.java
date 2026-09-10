package com.autoconsultancy.dto.request;

import lombok.Data;

@Data
public class ApplicationRequest {
    /** Optional: if coming from Browse Bikes → Apply Now (legacy BikeModel flow) */
    private Long bikeModelId;

    /** New: links to actual BikeInventory item selected by customer */
    private Long bikeInventoryId;
}
