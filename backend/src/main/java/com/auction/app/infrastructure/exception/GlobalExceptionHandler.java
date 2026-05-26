package com.auction.app.infrastructure.exception;

import java.time.Instant;

import com.auction.app.domains.auction.exceptions.*;
import com.auction.app.domains.auth.exceptions.*;
import com.auction.app.domains.feedback.exceptions.FeedBackNotFoundException;
import com.auction.app.domains.products.exceptions.ProductNotFoundException;
import com.auction.app.domains.transaction.exceptions.InsufficientFundsException;
import com.auction.app.domains.transaction.exceptions.InvalidTransactionStateException;
import com.auction.app.domains.transaction.exceptions.TransactionNotFoundException;
import com.auction.app.domains.transaction.exceptions.UnauthorizedTransactionException;
import com.auction.app.domains.users.exceptions.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Other
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(Exception ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    // Validation
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class
    })
    public ResponseEntity<ErrorResponse> handleValidationExceptions(Exception ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }

    // Security & Authorization
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.FORBIDDEN, request); // 403
    }

    @ExceptionHandler({
            RefreshTokenExpiredException.class,
            RefreshTokenSuspiciousActivityException.class,
            BadCredentialsException.class
    })
    public ResponseEntity<ErrorResponse> handleUnauthorizedRefreshExceptions(RuntimeException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.UNAUTHORIZED, request); // 401
    }

    @ExceptionHandler({
            RefreshTokenNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleUserNotFound(RuntimeException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, request); // 404
    }

    @ExceptionHandler({
            InvalidVerificationCodeException.class,
            InvalidPasswordResetFlowException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequestAuthExceptions(RuntimeException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request); // 400
    }

    @ExceptionHandler(EmailSendFailureException.class)
    public ResponseEntity<ErrorResponse> handleEmailFailure(EmailSendFailureException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, request); // 500
    }

    // Auction domain (main system)
    @ExceptionHandler({
            AuctionNotFoundException.class,
            BidNotFoundException.class,
    })
    public ResponseEntity<ErrorResponse> handleAuctionNotFound(RuntimeException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, request); // 404
    }

    @ExceptionHandler({
            InvalidEndTimeException.class,
            ListedProductException.class,
            InvalidProductQuantity.class,
            NotUpcommingAuctionException.class,
            InvalidBidException.class,
            InsufficientBalanceException.class
    })
    public ResponseEntity<ErrorResponse> handleAuctionValidationExceptions(RuntimeException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request); // 400
    }

    // User domain
    @ExceptionHandler({
            UserNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, request); // 404
    }

    @ExceptionHandler({
            InvalidPasswordException.class,
            InvalidUserStateException.class,
            UserUpdateException.class,
            SelfFollowException.class
    })
    public ResponseEntity<ErrorResponse> handleUserValidationExceptions(RuntimeException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }

    // Transaction domain
    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTransactionNotFound(TransactionNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, request); // 404
    }

    @ExceptionHandler(UnauthorizedTransactionException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedTransaction(UnauthorizedTransactionException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.FORBIDDEN, request); // 403
    }

    @ExceptionHandler({
            InsufficientFundsException.class,
            InvalidTransactionStateException.class
    })
    public ResponseEntity<ErrorResponse> handleTransactionValidationExceptions(RuntimeException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request); // 400
    }

    // Product domain
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProductNotFound(ProductNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, request); // 404
    }

    // Feedback domain
    @ExceptionHandler(FeedBackNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFeedbackNotFound(FeedBackNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, request); // 404
    }

    // Helper
    private ResponseEntity<ErrorResponse> buildErrorResponse(Exception ex, HttpStatus status, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, status);
    }
}