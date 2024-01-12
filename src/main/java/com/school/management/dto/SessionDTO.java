package com.school.management.dto;

import lombok.*;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionDTO {
    private String title;
    private String sessionType;
    private String feedbackLink;
    private Boolean isFinished;
    private Date sessionTimeStart;
    private Date sessionTimeEnd;
    private Long groupId;
    private Long teacherId;
    private Long roomId;

}
