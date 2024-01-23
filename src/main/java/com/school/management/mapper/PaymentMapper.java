package com.school.management.mapper;

import com.school.management.dto.PaymentDTO;
import com.school.management.persistance.PaymentEntity;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = false))
public interface PaymentMapper {

    @Mapping(source = "student.id", target = "studentId")
    @Mapping(source = "session.id", target = "sessionId")
    @Mapping(source = "sessionSeries.id", target = "sessionSeriesId")
    @Mapping(source = "group.id", target = "groupId")
    PaymentDTO toDto(PaymentEntity paymentEntity);

    PaymentEntity toEntity(PaymentDTO paymentDTO);



}