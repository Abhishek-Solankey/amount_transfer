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

        verify(notificationService).notifyAboutTransfer(accountFrom, "Amount credited : 200 to account 2");
        verify(notificationService).notifyAboutTransfer(accountTo, "Amount debited : 200 from account 1");

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
    void testParallelTransfers() throws InterruptedException {
        Account accountFrom1 = new Account("1", new BigDecimal("1000"));
        Account accountTo1 = new Account("2", new BigDecimal("500"));
        Account accountFrom2 = new Account("3", new BigDecimal("1000"));
        Account accountTo2 = new Account("4", new BigDecimal("500"));

        when(accountsRepository.getAccount("1")).thenReturn(accountFrom1);
        when(accountsRepository.getAccount("2")).thenReturn(accountTo1);
        when(accountsRepository.getAccount("3")).thenReturn(accountFrom2);
        when(accountsRepository.getAccount("4")).thenReturn(accountTo2);

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        executorService.submit(() -> accountsService.transferMoney("1", "2", new BigDecimal("100")));
        executorService.submit(() -> accountsService.transferMoney("3", "4", new BigDecimal("100")));

        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);

        // Validate that the balances are correctly updated
        assertEquals(new BigDecimal("900"), accountFrom1.getBalance());
        assertEquals(new BigDecimal("600"), accountTo1.getBalance());
        assertEquals(new BigDecimal("900"), accountFrom2.getBalance());
        assertEquals(new BigDecimal("600"), accountTo2.getBalance());

        // Verify notifications and account updates
        verify(notificationService).notifyAboutTransfer(accountFrom1, "Amount credited : 100 to account 2");
        verify(notificationService).notifyAboutTransfer(accountTo1, "Amount debited : 100 from account 1");
        verify(notificationService).notifyAboutTransfer(accountFrom2, "Amount credited : 100 to account 4");
        verify(notificationService).notifyAboutTransfer(accountTo2, "Amount debited : 100 from account 3");

        verify(accountsRepository).updateAccount(accountFrom1);
        verify(accountsRepository).updateAccount(accountTo1);
        verify(accountsRepository).updateAccount(accountFrom2);
        verify(accountsRepository).updateAccount(accountTo2);
    }

}
