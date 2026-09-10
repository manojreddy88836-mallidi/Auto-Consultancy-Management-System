package com.autoconsultancy.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OfferRespondRequest {

    /**
     * Action to take:
     * ACCEPTED       — accept the customer's offer as-is
     * REJECTED       — reject the offer
     * COUNTER_OFFER  — send a counter price
     */
    private String action;

    /** Required only when action = COUNTER_OFFER */
    private BigDecimal counterOfferPrice;

    /** Optional note to the customer explaining the decision */
    private String adminResponse;
}
