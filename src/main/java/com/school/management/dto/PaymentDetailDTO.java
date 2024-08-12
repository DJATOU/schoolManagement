package com.school.management.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDetailDTO {

    private Long paymentDetailId;
    private Long sessionId;
    private String sessionName;
    private Double amountPaid;
    private Double remainingBalance;
}

