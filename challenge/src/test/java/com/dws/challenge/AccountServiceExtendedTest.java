package com.dws.challenge;

import com.dws.challenge.domain.Account;
import com.dws.challenge.service.AccountsService;
import com.dws.challenge.service.EmailNotificationService;
import com.dws.challenge.repository.AccountsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class AccountServiceExtendedTest {

    @Autowired
    private AccountsService accountsService;

    @MockBean
    private AccountsRepository accountsRepository;

    @MockBean
    private EmailNotificationService notificationService;

    private Account accountFrom;
    private Account accountTo;

    @BeforeEach
    void setUp() {
        // Initialize accounts with sufficient balance
        accountFrom = new Account("1", new BigDecimal("1000"));
        accountTo = new Account("2", new BigDecimal("500"));

        // Mock repository behavior
        when(accountsRepository.getAccount("1")).thenReturn(accountFrom);
        when(accountsRepository.getAccount("2")).thenReturn(accountTo);
    }

    @Test
    void testTransferMoney_success() {
        accountsService.transferMoney("1", "2", new BigDecimal("200"));

        assertEquals(new BigDecimal("800"), accountFrom.getBalance());
        assertEquals(new BigDecimal("700"), accountTo.getBalance());

        verify(notificationService).notifyAboutTransfer(accountFrom, "Amount transferred : 200 to account 2");
        verify(notificationService).notifyAboutTransfer(accountTo, "Amount transferred : 200 from account 1");

        verify(accountsRepository).updateAccount(accountFrom);
        verify(accountsRepository).updateAccount(accountTo);
    }

    @Test
    void testTransferMoney_insufficientBalance() {
        assertThrows(IllegalArgumentException.class, () -> {
            accountsService.transferMoney("1", "2", new BigDecimal("2000"));
        });

        assertEquals(new BigDecimal("1000"), accountFrom.getBalance());
        assertEquals(new BigDecimal("500"), accountTo.getBalance());

        verify(notificationService, never()).notifyAboutTransfer(any(), anyString());
        verify(accountsRepository, never()).updateAccount(any());
    }

    @Test
    void testTransferMoney_negativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> {
            accountsService.transferMoney("1", "2", new BigDecimal("-100"));
        });

        assertEquals(new BigDecimal("1000"), accountFrom.getBalance());
        assertEquals(new BigDecimal("500"), accountTo.getBalance());

        verify(notificationService, never()).notifyAboutTransfer(any(), anyString());
        verify(accountsRepository, never()).updateAccount(any());
    }

    @Test
    void testConcurrentTransfers() throws InterruptedException {
        // Use an ExecutorService to simulate concurrent transfers
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        // Perform 10 concurrent transfers, each transferring 100
        for (int i = 0; i < 10; i++) {
            executorService.submit(() -> accountsService.transferMoney("1", "2", new BigDecimal("100")));
        }

        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);

        // After 10 concurrent transfers of 100, accountFrom should have 0, accountTo should have 1500
        assertEquals(new BigDecimal("0"), accountFrom.getBalance());
        assertEquals(new BigDecimal("1500"), accountTo.getBalance());

        // Verify that notifications were sent 10 times for each account
        verify(notificationService, times(10)).notifyAboutTransfer(eq(accountFrom), contains("Amount transferred : 100 to account 2"));
        verify(notificationService, times(10)).notifyAboutTransfer(eq(accountTo), contains("Amount transferred : 100 from account 1"));

        // Verify that accounts were updated 10 times in the repository
        verify(accountsRepository, times(10)).updateAccount(accountFrom);
        verify(accountsRepository, times(10)).updateAccount(accountTo);
    }
}
