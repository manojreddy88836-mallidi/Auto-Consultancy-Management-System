package com.autoconsultancy.dto.request;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CreateWorkerRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String password;
    private String employeeId;
    private String department;
    private String designation;
    private LocalDate joiningDate;
}
