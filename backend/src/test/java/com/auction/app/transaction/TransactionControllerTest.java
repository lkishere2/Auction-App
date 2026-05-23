package com.auction.app.transaction;

import com.auction.app.domains.transaction.dtos.ClientRequest;
import com.auction.app.domains.transaction.TransactionController;
import com.auction.app.domains.transaction.dtos.TransactionRequest;
import com.auction.app.domains.transaction.dtos.TransactionResponse;
import com.auction.app.domains.transaction.TransactionService;
import com.auction.app.domains.transaction.TransactionStatus;
import com.auction.app.domains.transaction.TransactionType;
import com.auction.app.infrastructure.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = TransactionController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransactionService transactionService;

    // =========================================================================
    // METHOD 1: getUserTransactions (5 Tests)
    // =========================================================================

    // --- Happy Paths (3 Tests) ---

    @Test
    void getUserTransactions_WhenCalled_ShouldReturnTransactionPage() throws Exception {
        when(transactionService.getUserTransaction(0, 10))
                .thenReturn(new PageImpl<>(List.of(createTransactionResponse("100.00", TransactionType.DEPOSIT, TransactionStatus.PENDING))));

        mockMvc.perform(get("/api/v1/transaction/me")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].amount").value(100.00))
                .andExpect(jsonPath("$.content[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));

        verify(transactionService).getUserTransaction(0, 10);
    }

    @Test
    void getUserTransactions_WhenParamsAreMissing_ShouldUseDefaultPagination() throws Exception {
        when(transactionService.getUserTransaction(0, 10)).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/transaction/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        verify(transactionService).getUserTransaction(0, 10);
    }

    @Test
    void getUserTransactions_WhenBoundarySizeOne_ShouldPassParamsToService() throws Exception {
        when(transactionService.getUserTransaction(0, 1))
                .thenReturn(new PageImpl<>(List.of(createTransactionResponse("1.00", TransactionType.DEPOSIT, TransactionStatus.PENDING))));

        mockMvc.perform(get("/api/v1/transaction/me")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].amount").value(1.00));

        verify(transactionService).getUserTransaction(0, 1);
    }

    // --- Edge Cases (2 Tests) ---

    @Test
    void getUserTransactions_WhenPageIsNegative_CurrentControllerStillPassesParamsToService() throws Exception {
        when(transactionService.getUserTransaction(-1, 10)).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/transaction/me")
                        .param("page", "-1")
                        .param("size", "10"))
                .andExpect(status().isOk());

        verify(transactionService).getUserTransaction(-1, 10);
    }

    @Test
    void getUserTransactions_WhenSizeIsZero_CurrentControllerStillPassesParamsToService() throws Exception {
        when(transactionService.getUserTransaction(0, 0)).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/transaction/me")
                        .param("page", "0")
                        .param("size", "0"))
                .andExpect(status().isOk());

        verify(transactionService).getUserTransaction(0, 0);
    }

    // =========================================================================
    // METHOD 2: createTransaction (5 Tests)
    // =========================================================================

    // --- Happy Paths (2 Tests) ---

    @Test
    void createTransaction_WhenDepositRequestIsValid_ShouldReturnCreatedTransaction() throws Exception {
        TransactionRequest request = createTransactionRequest("100.00", TransactionType.DEPOSIT);
        when(transactionService.createTransaction(any(TransactionRequest.class)))
                .thenReturn(createTransactionResponse("100.00", TransactionType.DEPOSIT, TransactionStatus.PENDING));

        mockMvc.perform(post("/api/v1/transaction/create")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(transactionService).createTransaction(any(TransactionRequest.class));
    }

    @Test
    void createTransaction_WhenWithdrawalRequestHasBoundaryAmount_ShouldReturnCreatedTransaction() throws Exception {
        TransactionRequest request = createTransactionRequest("0.01", TransactionType.WITHDRAWAL);
        when(transactionService.createTransaction(any(TransactionRequest.class)))
                .thenReturn(createTransactionResponse("0.01", TransactionType.WITHDRAWAL, TransactionStatus.PENDING));

        mockMvc.perform(post("/api/v1/transaction/create")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(0.01))
                .andExpect(jsonPath("$.type").value("WITHDRAWAL"));

        verify(transactionService).createTransaction(any(TransactionRequest.class));
    }

    // --- Edge Cases (3 Tests) ---

    @Test
    void createTransaction_WhenBodyIsMissing_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/transaction/create")
                        .contentType("application/json"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }

    @Test
    void createTransaction_WhenTypeIsInvalid_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/transaction/create")
                        .contentType("application/json")
                        .content("{\"amount\":100,\"type\":\"NOT_A_TYPE\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }

    @Test
    void createTransaction_WhenAmountIsNegative_CurrentControllerStillPassesRequestToService() throws Exception {
        TransactionRequest request = createTransactionRequest("-1.00", TransactionType.DEPOSIT);
        when(transactionService.createTransaction(any(TransactionRequest.class)))
                .thenReturn(createTransactionResponse("-1.00", TransactionType.DEPOSIT, TransactionStatus.PENDING));

        mockMvc.perform(post("/api/v1/transaction/create")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(-1.00));

        verify(transactionService).createTransaction(any(TransactionRequest.class));
    }

    // =========================================================================
    // METHOD 3: deleteTransaction (3 Tests)
    // =========================================================================

    // --- Happy Path (1 Test) ---

    @Test
    void deleteTransaction_WhenIdExists_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/transaction/delete/{id}", 10L))
                .andExpect(status().isNoContent());

        verify(transactionService).deleteTransaction(10L);
    }

    // --- Edge Cases (2 Tests) ---

    @Test
    void deleteTransaction_WhenIdIsZero_CurrentControllerStillPassesIdToService() throws Exception {
        mockMvc.perform(delete("/api/v1/transaction/delete/{id}", 0L))
                .andExpect(status().isNoContent());

        verify(transactionService).deleteTransaction(0L);
    }

    @Test
    void deleteTransaction_WhenIdIsNotNumber_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(delete("/api/v1/transaction/delete/not-a-number"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }

    // =========================================================================
    // METHOD 4: getAllTransactionRequests (4 Tests)
    // =========================================================================

    // --- Happy Paths (2 Tests) ---

    @Test
    void getAllTransactionRequests_WhenCalled_ShouldReturnClientRequestPage() throws Exception {
        when(transactionService.getAllTransactionRequest(0, 10))
                .thenReturn(new PageImpl<>(List.of(createClientRequest(10L, 1L, "100.00", TransactionType.DEPOSIT))));

        mockMvc.perform(get("/api/v1/transaction/all")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].transactionId").value(10))
                .andExpect(jsonPath("$.content[0].userId").value(1))
                .andExpect(jsonPath("$.content[0].type").value("DEPOSIT"));

        verify(transactionService).getAllTransactionRequest(0, 10);
    }

    @Test
    void getAllTransactionRequests_WhenParamsAreMissing_ShouldUseDefaults() throws Exception {
        when(transactionService.getAllTransactionRequest(0, 10)).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/transaction/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        verify(transactionService).getAllTransactionRequest(0, 10);
    }

    // --- Edge Cases (2 Tests) ---

    @Test
    void getAllTransactionRequests_WhenPageIsNegative_CurrentControllerStillPassesParamsToService() throws Exception {
        when(transactionService.getAllTransactionRequest(-1, 10)).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/transaction/all")
                        .param("page", "-1")
                        .param("size", "10"))
                .andExpect(status().isOk());

        verify(transactionService).getAllTransactionRequest(-1, 10);
    }

    @Test
    void getAllTransactionRequests_WhenSizeIsZero_CurrentControllerStillPassesParamsToService() throws Exception {
        when(transactionService.getAllTransactionRequest(0, 0)).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/transaction/all")
                        .param("page", "0")
                        .param("size", "0"))
                .andExpect(status().isOk());

        verify(transactionService).getAllTransactionRequest(0, 0);
    }

    // =========================================================================
    // METHOD 5: acceptTransaction (4 Tests)
    // =========================================================================

    // --- Happy Paths (2 Tests) ---

    @Test
    void acceptTransaction_WhenDepositRequestIsValid_ShouldReturnNoContent() throws Exception {
        ClientRequest request = createClientRequest(10L, 1L, "100.00", TransactionType.DEPOSIT);

        mockMvc.perform(post("/api/v1/transaction/deposit")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(transactionService).acceptTransaction(any(ClientRequest.class));
    }

    @Test
    void acceptTransaction_WhenWithdrawalRequestIsValid_ShouldReturnNoContent() throws Exception {
        ClientRequest request = createClientRequest(10L, 1L, "25.00", TransactionType.WITHDRAWAL);

        mockMvc.perform(post("/api/v1/transaction/deposit")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(transactionService).acceptTransaction(any(ClientRequest.class));
    }

    // --- Edge Cases (2 Tests) ---

    @Test
    void acceptTransaction_WhenBodyIsMissing_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/transaction/deposit")
                        .contentType("application/json"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }

    @Test
    void acceptTransaction_WhenTypeIsInvalid_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/transaction/deposit")
                        .contentType("application/json")
                        .content("{\"transactionId\":10,\"userId\":1,\"amount\":100,\"type\":\"NOT_A_TYPE\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }

    // =========================================================================
    // METHOD 6: cancelTransaction (3 Tests)
    // =========================================================================

    // --- Happy Path (1 Test) ---

    @Test
    void cancelTransaction_WhenIdExists_ShouldReturnOk() throws Exception {
        mockMvc.perform(put("/api/v1/transaction/cancel/{id}", 10L))
                .andExpect(status().isOk());

        verify(transactionService).cancelTransaction(10L);
    }

    // --- Edge Cases (2 Tests) ---

    @Test
    void cancelTransaction_WhenIdIsZero_CurrentControllerStillPassesIdToService() throws Exception {
        mockMvc.perform(put("/api/v1/transaction/cancel/{id}", 0L))
                .andExpect(status().isOk());

        verify(transactionService).cancelTransaction(0L);
    }

    @Test
    void cancelTransaction_WhenIdIsNotNumber_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(put("/api/v1/transaction/cancel/not-a-number"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }

    // =========================================================================
    // METHOD 7: unhandled service exceptions (2 Tests)
    // =========================================================================

    // --- Edge Cases (2 Tests) ---

    @Test
    void deleteTransaction_WhenServiceThrows_CurrentControllerPropagatesException() {
        org.mockito.Mockito.doThrow(new RuntimeException("Transaction not found"))
                .when(transactionService).deleteTransaction(99L);

        assertThatThrownBy(() -> mockMvc.perform(delete("/api/v1/transaction/delete/{id}", 99L)))
                .isInstanceOf(jakarta.servlet.ServletException.class)
                .hasRootCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void acceptTransaction_WhenServiceThrows_CurrentControllerPropagatesException() {
        org.mockito.Mockito.doThrow(new RuntimeException("Insufficient funds for withdrawal."))
                .when(transactionService).acceptTransaction(any(ClientRequest.class));

        assertThatThrownBy(() -> mockMvc.perform(post("/api/v1/transaction/deposit")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(createClientRequest(10L, 1L, "999.00", TransactionType.WITHDRAWAL)))))
                .isInstanceOf(jakarta.servlet.ServletException.class)
                .hasRootCauseInstanceOf(RuntimeException.class);
    }

    private TransactionRequest createTransactionRequest(String amount, TransactionType type) {
        TransactionRequest request = new TransactionRequest();
        request.setAmount(new BigDecimal(amount));
        request.setType(type);
        return request;
    }

    private TransactionResponse createTransactionResponse(String amount, TransactionType type, TransactionStatus status) {
        return TransactionResponse.builder()
                .amount(new BigDecimal(amount))
                .type(type)
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private ClientRequest createClientRequest(Long transactionId, Long userId, String amount, TransactionType type) {
        return ClientRequest.builder()
                .transactionId(transactionId)
                .userId(userId)
                .username("client")
                .email("client@example.com")
                .amount(new BigDecimal(amount))
                .type(type)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
