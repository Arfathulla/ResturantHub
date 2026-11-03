package com.restauranthub.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restauranthub.entity.*;
import com.restauranthub.entity.enums.OrderStatus;
import com.restauranthub.entity.enums.PaymentProvider;
import com.restauranthub.entity.enums.PaymentStatus;
import com.restauranthub.entity.enums.ReservationStatus;
import com.restauranthub.exception.BadRequestException;
import com.restauranthub.exception.ResourceNotFoundException;
import com.restauranthub.exception.UnauthorizedException;
import com.restauranthub.payment.*;
import com.restauranthub.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final PaymentWebhookEventRepository webhookEventRepository;
    private final OrderRepository orderRepository;
    private final TableReservationRepository tableReservationRepository;
    private final RoomReservationRepository roomReservationRepository;
    private final HallReservationRepository hallReservationRepository;
    private final ParkingReservationRepository parkingReservationRepository;
    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;

    @Value("${app.payment.provider:MOCK}")
    private String configuredProvider;

    public PaymentGatewayService getGateway(PaymentProvider provider) {
        return switch (provider) {
            case STRIPE -> applicationContext.getBean("stripePaymentGateway", PaymentGatewayService.class);
            case RAZORPAY -> applicationContext.getBean("razorpayPaymentGateway", PaymentGatewayService.class);
            case MOCK -> applicationContext.getBean("mockPaymentGateway", PaymentGatewayService.class);
        };
    }

    public PaymentProvider getCurrentProvider() {
        try {
            return PaymentProvider.valueOf(configuredProvider.toUpperCase());
        } catch (Exception e) {
            return PaymentProvider.MOCK;
        }
    }

    @Transactional
    public PaymentResponse initiateOrderPayment(Long orderId, User user, String idempotencyKey) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Cannot pay for another user's order.");
        }

        // Idempotency check: Return existing payment if already generated
        Optional<Payment> existingPayment = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existingPayment.isPresent()) {
            Payment p = existingPayment.get();
            return PaymentResponse.builder()
                    .success(p.getStatus() == PaymentStatus.COMPLETED || p.getStatus() == PaymentStatus.PENDING)
                    .gatewayTransactionId(p.getGatewayTransactionId())
                    .message("Returning existing idempotent payment: " + p.getPaymentReference())
                    .build();
        }

        PaymentProvider provider = getCurrentProvider();
        String paymentRef = "PAY-" + LocalDate.now().toString().replace("-", "") + "-" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        PaymentGatewayService gateway = getGateway(provider);
        PaymentRequest request = PaymentRequest.builder()
                .paymentReference(paymentRef)
                .amount(order.getFinalTotal())
                .currency("USD")
                .customerEmail(user.getEmail())
                .description("Payment for Order " + order.getOrderNumber())
                .build();

        PaymentResponse gatewayResponse = gateway.initiatePayment(request);

        Payment payment = Payment.builder()
                .paymentReference(paymentRef)
                .user(user)
                .order(order)
                .amount(order.getFinalTotal())
                .currency("USD")
                .provider(provider)
                .gatewayTransactionId(gatewayResponse.getGatewayTransactionId())
                .status(gatewayResponse.isSuccess() ? PaymentStatus.PENDING : PaymentStatus.FAILED)
                .idempotencyKey(idempotencyKey)
                .build();

        paymentRepository.save(payment);

        // In Mock auto-succeed mode, transition payment to complete immediately for seamless testing
        if (provider == PaymentProvider.MOCK && gatewayResponse.isSuccess()) {
            handlePaymentSuccess(paymentRef, gatewayResponse.getGatewayTransactionId());
        }

        return gatewayResponse;
    }

    @Transactional
    public PaymentResponse initiateTableDeposit(Long reservationId, User user, String idempotencyKey) {
        TableReservation res = tableReservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("TableReservation", "id", reservationId));

        if (!res.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Cannot pay deposit for another user's reservation.");
        }

        Optional<Payment> existingPayment = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existingPayment.isPresent()) {
            Payment p = existingPayment.get();
            return PaymentResponse.builder()
                    .success(true)
                    .gatewayTransactionId(p.getGatewayTransactionId())
                    .message("Existing idempotent payment found")
                    .build();
        }

        PaymentProvider provider = getCurrentProvider();
        String paymentRef = "DEP-T-" + LocalDate.now().toString().replace("-", "") + "-" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        PaymentGatewayService gateway = getGateway(provider);
        PaymentRequest request = PaymentRequest.builder()
                .paymentReference(paymentRef)
                .amount(res.getDepositAmount())
                .currency("USD")
                .customerEmail(user.getEmail())
                .description("Deposit for Table Reservation " + res.getReferenceNo())
                .build();

        PaymentResponse gatewayResponse = gateway.initiatePayment(request);

        Payment payment = Payment.builder()
                .paymentReference(paymentRef)
                .user(user)
                .tableReservation(res)
                .amount(res.getDepositAmount())
                .currency("USD")
                .provider(provider)
                .gatewayTransactionId(gatewayResponse.getGatewayTransactionId())
                .status(gatewayResponse.isSuccess() ? PaymentStatus.PENDING : PaymentStatus.FAILED)
                .idempotencyKey(idempotencyKey)
                .build();

        paymentRepository.save(payment);

        if (provider == PaymentProvider.MOCK && gatewayResponse.isSuccess()) {
            handlePaymentSuccess(paymentRef, gatewayResponse.getGatewayTransactionId());
        }

        return gatewayResponse;
    }

    @Transactional
    public void handlePaymentSuccess(String paymentReference, String gatewayTxId) {
        Payment payment = paymentRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "paymentReference", paymentReference));

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            log.info("Payment reference {} already completed. Ignoring duplicate.", paymentReference);
            return;
        }

        payment.setStatus(PaymentStatus.COMPLETED);
        if (gatewayTxId != null) {
            payment.setGatewayTransactionId(gatewayTxId);
        }
        paymentRepository.save(payment);

        // Update corresponding business entity state
        if (payment.getOrder() != null) {
            Order order = payment.getOrder();
            order.setStatus(OrderStatus.PLACED);
            orderRepository.save(order);
            log.info("Order {} confirmed via payment {}", order.getOrderNumber(), paymentReference);
        }

        if (payment.getTableReservation() != null) {
            TableReservation tr = payment.getTableReservation();
            tr.setStatus(ReservationStatus.CONFIRMED);
            tr.setDepositPaid(true);
            tableReservationRepository.save(tr);
            log.info("Table reservation {} confirmed via deposit payment {}", tr.getReferenceNo(), paymentReference);
        }

        if (payment.getRoomReservation() != null) {
            RoomReservation rr = payment.getRoomReservation();
            rr.setStatus(ReservationStatus.CONFIRMED);
            roomReservationRepository.save(rr);
        }

        if (payment.getHallReservation() != null) {
            HallReservation hr = payment.getHallReservation();
            hr.setStatus(ReservationStatus.CONFIRMED);
            hallReservationRepository.save(hr);
        }

        if (payment.getParkingReservation() != null) {
            ParkingReservation pr = payment.getParkingReservation();
            pr.setStatus(ReservationStatus.CONFIRMED);
            parkingReservationRepository.save(pr);
        }
    }

    @Transactional
    public void processWebhookEvent(String providerName, String eventId, String payload, String signatureHeader) {
        // Idempotency: Reject already processed webhooks
        if (webhookEventRepository.existsByEventId(eventId)) {
            log.info("Duplicate webhook event ID {} received. Ignoring.", eventId);
            return;
        }

        PaymentProvider provider = PaymentProvider.valueOf(providerName.toUpperCase());
        PaymentGatewayService gateway = getGateway(provider);

        if (!gateway.verifyWebhookSignature(payload, signatureHeader)) {
            log.error("Webhook signature verification failed for provider: {}, eventId: {}", providerName, eventId);
            throw new BadRequestException("INVALID_SIGNATURE", "Webhook signature verification failed.");
        }

        PaymentWebhookEvent event = PaymentWebhookEvent.builder()
                .eventId(eventId)
                .provider(providerName)
                .payload(payload)
                .status("PROCESSED")
                .build();
        webhookEventRepository.save(event);

        try {
            JsonNode root = objectMapper.readTree(payload);
            String paymentRef = root.has("paymentReference") ? root.get("paymentReference").asText() : null;
            String gatewayTxId = root.has("transactionId") ? root.get("transactionId").asText() : null;

            if (paymentRef != null) {
                handlePaymentSuccess(paymentRef, gatewayTxId);
            }
        } catch (Exception ex) {
            log.error("Error extracting details from webhook payload: {}", ex.getMessage());
        }
    }

    @Transactional
    public Refund processRefund(String paymentReference, BigDecimal amount, String reason, User currentUser) {
        Payment payment = paymentRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "paymentReference", paymentReference));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new BadRequestException("INVALID_STATUS", "Only completed payments can be refunded.");
        }

        if (amount.compareTo(payment.getAmount()) > 0) {
            throw new BadRequestException("INVALID_AMOUNT", "Refund amount cannot exceed original payment amount.");
        }

        PaymentGatewayService gateway = getGateway(payment.getProvider());
        PaymentResponse response = gateway.processRefund(payment, amount, reason);

        String refundRef = "REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Refund refund = Refund.builder()
                .payment(payment)
                .refundReference(refundRef)
                .amount(amount)
                .reason(reason)
                .status(response.isSuccess() ? PaymentStatus.COMPLETED : PaymentStatus.FAILED)
                .gatewayRefundId(response.getGatewayTransactionId())
                .build();

        Refund savedRefund = refundRepository.save(refund);

        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        return savedRefund;
    }
}
