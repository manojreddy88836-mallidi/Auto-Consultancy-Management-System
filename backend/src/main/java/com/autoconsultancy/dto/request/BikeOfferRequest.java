package com.autoconsultancy.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BikeOfferRequest {

    /** The exact inventory bike being negotiated */
    private Long bikeInventoryId;

    /** Customer's offered price — must be > 0 */
    private BigDecimal offeredPrice;

    /** Optional message/reason from the customer */
    private String customerMessage;
}
