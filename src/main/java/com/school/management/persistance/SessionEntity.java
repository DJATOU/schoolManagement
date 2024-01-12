package com.school.management.persistance;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;
import java.util.Set;

@Entity
@Table(name = "session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title")
    private String title;

    @Column(name = "session_type")
    private String sessionType;

    @Column(name = "feedback_link") // A link to a feedback form or survey for the session
    private String feedbackLink;

    @Column(name = "is_finished")
    private Boolean isFinished;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private GroupEntity group;

    @ManyToOne
    @JoinColumn(name = "teacher_id")
    private TeacherEntity teacher;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "session_time_Start")
    private Date sessionTimeStart;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "session_time_end")
    private Date sessionTimeEnd;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private RoomEntity room;

    @OneToMany(mappedBy = "session")
    private Set<AttendanceEntity> attendances ;

    @ManyToOne
    @JoinColumn(name = "session_series_id")
    private SessionSeriesEntity sessionSeries;

}
