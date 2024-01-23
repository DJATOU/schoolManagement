package com.school.management.controller;

import com.school.management.dto.PaymentDTO;
import com.school.management.dto.StudentPaymentStatusDTO;
import com.school.management.persistance.*;
import com.school.management.repository.*;
import com.school.management.service.PatchService;
import com.school.management.service.PaymentService;
import com.school.management.service.exception.CustomServiceException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final StudentRepository studentRepository;
    private final SessionRepository sessionRepository;
    private final SessionSeriesRepository sessionSeriesRepository;

    private final GroupRepository groupRepository;

    private final PatchService patchService;

    @Autowired
    public PaymentController(PaymentService paymentService, StudentRepository studentRepository,
                             SessionRepository sessionRepository, SessionSeriesRepository sessionSeriesRepository, GroupRepository groupRepository, PatchService patchService){
        this.paymentService = paymentService;
        this.studentRepository = studentRepository;
        this.sessionRepository = sessionRepository;
        this.sessionSeriesRepository = sessionSeriesRepository;
        this.groupRepository = groupRepository;
        this.patchService = patchService;
    }

    @PostMapping
    public ResponseEntity<PaymentDTO> createPayment(@Valid @RequestBody PaymentDTO paymentDto) {
        PaymentEntity payment = convertToEntity(paymentDto);
        PaymentEntity savedPayment = paymentService.createPayment(payment);
        return new ResponseEntity<>(convertToDto(savedPayment), HttpStatus.CREATED);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PaymentDTO> patchPayment(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        PaymentEntity payment = paymentService.getPaymentById(id);
        patchService.applyPatch(payment, updates);
        PaymentEntity updatedPayment = paymentService.save(payment);
        return ResponseEntity.ok(convertToDto(updatedPayment));
    }

    @GetMapping
    public ResponseEntity<List<PaymentDTO>> getAllPayments() {
        List<PaymentEntity> payments = paymentService.getAllPayments();
        List<PaymentDTO> allPaymentDto = payments.stream().map(this::convertToDto).toList();
        return new ResponseEntity<>(allPaymentDto, HttpStatus.OK);
    }

    // Get all payments for a student
    @Transactional
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<PaymentDTO>> getAllPaymentsForStudent(@PathVariable Long studentId) {
        List<PaymentEntity> payments = paymentService.getAllPaymentsForStudent(studentId);
        List<PaymentDTO> allPaymentDto = payments.stream().map(this::convertToDto).toList();
        return new ResponseEntity<>(allPaymentDto, HttpStatus.OK);
    }

    //put mapping for payment
    @PutMapping("/{id}")
    public ResponseEntity<PaymentDTO> updatePayment(@PathVariable Long id) {
        PaymentEntity updatedPayment = paymentService.updatePayment(id);
        return new ResponseEntity<>(convertToDto(updatedPayment), HttpStatus.OK);
    }


    @PostMapping("/process")
    public ResponseEntity<PaymentDTO> processPayment(@Valid @RequestBody PaymentDTO paymentDto) {
        // Convertir PaymentDTO en PaymentEntity
        PaymentEntity paymentEntity = convertToEntity(paymentDto);

        // Appeler le service pour traiter le paiement
        PaymentEntity processedPayment = paymentService.processPayment(
                paymentEntity.getStudent().getId(),
                paymentEntity.getGroup().getId(),
                paymentEntity.getSessionSeries().getId(),
                paymentEntity.getAmountPaid()
        );

        // Convertir le paiement traité en DTO pour la réponse
        PaymentDTO responseDto = convertToDto(processedPayment);

        return ResponseEntity.ok(responseDto);
    }



    // DTO conversion methods
    private PaymentDTO convertToDto(PaymentEntity payment) {
        PaymentDTO paymentDto = new PaymentDTO();

        paymentDto.setStudentId(payment.getStudent() != null ? payment.getStudent().getId() : null);
        paymentDto.setSessionId(payment.getSession() != null ? payment.getSession().getId() : null);
        paymentDto.setAmountPaid(payment.getAmountPaid());
        paymentDto.setPaymentForMonth(payment.getPaymentForMonth());
        paymentDto.setStatus(payment.getStatus());
        paymentDto.setPaymentMethod(payment.getPaymentMethod());
        paymentDto.setPaymentDescription(payment.getDescription());
        paymentDto.setSessionSeriesId(payment.getSessionSeries() != null ? payment.getSessionSeries().getId() : null);

        // Add other fields from PaymentEntity to PaymentDTO as needed

        return paymentDto;
    }


    private PaymentEntity convertToEntity(PaymentDTO paymentDto) {
        PaymentEntity payment = new PaymentEntity();

        // Set the student for this payment
        if (paymentDto.getStudentId() != null) {
            StudentEntity student = studentRepository.findById(paymentDto.getStudentId())
                    .orElseThrow(() -> new CustomServiceException("Student not found with ID: " + paymentDto.getStudentId()));
            payment.setStudent(student);
        }

        // Set the session if provided
        if (paymentDto.getSessionId() != null) {
            SessionEntity session = sessionRepository.findById(paymentDto.getSessionId())
                    .orElseThrow(() -> new CustomServiceException("Session not found with ID: " + paymentDto.getSessionId()));
            payment.setSession(session);
        }

        // Set the session series if provided
        if (paymentDto.getSessionSeriesId() != null) {
            SessionSeriesEntity sessionSeries = sessionSeriesRepository.findById(paymentDto.getSessionSeriesId())
                    .orElseThrow(() -> new CustomServiceException("Session series not found with ID: " + paymentDto.getSessionSeriesId()));
            payment.setSessionSeries(sessionSeries);
        }

        if (paymentDto.getGroupId() != null) {
            GroupEntity group = groupRepository.findById(paymentDto.getGroupId())
                    .orElseThrow(() -> new CustomServiceException("Group not found"));
            payment.setGroup(group);
        }

        // Set other fields from the DTO
        payment.setAmountPaid(paymentDto.getAmountPaid());
        payment.setPaymentForMonth(paymentDto.getPaymentForMonth());
        payment.setStatus(paymentDto.getStatus());
        payment.setPaymentMethod(paymentDto.getPaymentMethod());
        payment.setDescription(paymentDto.getPaymentDescription());

        // Return the populated PaymentEntity
        return payment;
    }

    @GetMapping("/{groupId}/students-payment-status")
    public ResponseEntity<List<StudentPaymentStatusDTO>> getStudentsPaymentStatus(@PathVariable Long groupId) {
        List<StudentPaymentStatusDTO> paymentStatusDTOList = paymentService.getPaymentStatusForGroup(groupId).stream()
                .map(status -> new StudentPaymentStatusDTO(status.getStudentId(),
                        status.getStudentName(), status.isPaymentOverdue()))
                .toList();
        return ResponseEntity.ok(paymentStatusDTOList);
    }

}
