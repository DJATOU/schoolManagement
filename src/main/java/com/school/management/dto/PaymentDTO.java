package com.school.management.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDTO {

    @NotNull(message = "Student ID cannot be null")
    private Long studentId;

    @NotNull(message = "Session ID cannot be null")
    private Long sessionId;

    @NotNull(message = "Session Series ID cannot be null")
    private Long sessionSeriesId;

    @NotNull(message = "Amount paid cannot be null")
    @Min(value = 0, message = "Amount paid must be greater than or equal to 0")
    private Double amountPaid;

    private Date paymentForMonth;
    private String status;
    private String paymentMethod;
    private String paymentDescription;

    @NotNull(message = "Group ID cannot be null")
    private Long groupId;


}
