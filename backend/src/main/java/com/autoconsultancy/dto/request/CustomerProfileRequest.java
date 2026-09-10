package com.autoconsultancy.dto.request;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CustomerProfileRequest {
    private String firstName;
    private String lastName;
    private String phone;
    private LocalDate dateOfBirth;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String identityProof;
    private String identityProofNumber;
}
