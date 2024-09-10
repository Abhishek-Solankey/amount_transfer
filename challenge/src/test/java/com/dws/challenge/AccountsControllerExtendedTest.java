package com.dws.challenge;

import com.dws.challenge.service.AccountsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountsControllerExtendedTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountsService accountsService;

    @BeforeEach
    void setUp() {
        // Initialize if necessary
    }

    @Test
    void testTransferMoney_success() throws Exception {
        mockMvc.perform(post("/v1/accounts/transfer")
                        .param("fromAccountId", "1")
                        .param("toAccountId", "2")
                        .param("amount", "100")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string("Transfer is successful "));

        verify(accountsService).transferMoney("1", "2", new BigDecimal("100"));
    }

    @Test
    void testTransferMoney_insufficientFunds() throws Exception {
        doThrow(new IllegalArgumentException("Insufficient balance"))
                .when(accountsService).transferMoney("1", "2", new BigDecimal("500"));

        mockMvc.perform(post("/v1/accounts/transfer")
                        .param("fromAccountId", "1")
                        .param("toAccountId", "2")
                        .param("amount", "500")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Insufficient balance"));

        verify(accountsService).transferMoney("1", "2", new BigDecimal("500"));
    }

    @Test
    void testTransferMoney_negativeAmount() throws Exception {
        doThrow(new IllegalArgumentException("Transfer amount must be positive"))
                .when(accountsService).transferMoney("1", "2", new BigDecimal("-100"));

        mockMvc.perform(post("/v1/accounts/transfer")
                        .param("fromAccountId", "1")
                        .param("toAccountId", "2")
                        .param("amount", "-100")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Transfer amount must be positive"));

        verify(accountsService).transferMoney("1", "2", new BigDecimal("-100"));
    }
}
