package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.repository.AccountsRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AccountsService {

  @Getter
  private final AccountsRepository accountsRepository;

  private final EmailNotificationService emailNotificationService;

  public AccountsService(AccountsRepository accountsRepository, EmailNotificationService emailNotificationService) {
    this.accountsRepository = accountsRepository;
    this.emailNotificationService = emailNotificationService;
  }

  public void createAccount(Account account) {
    this.accountsRepository.createAccount(account);
  }

  public Account getAccount(String accountId) {
    return this.accountsRepository.getAccount(accountId);
  }

  /**
   * Transfer amount between two accounts in a thread-safe manner
   * @param accountFromId Id of the source account
   * @param accountToId Id of target account
   * @param amount the amount to be transferred
   * @throws IllegalArgumentException if the amount is not positive or insufficient balance in account of source
   */
  public synchronized void transferMoney(String accountFromId, String accountToId, BigDecimal amount){
    if(amount.compareTo(BigDecimal.ZERO) <= 0){
      throw new IllegalArgumentException("Transfer amount must be positive");
    }

    Account accountFrom = this.accountsRepository.getAccount(accountFromId);
    Account accountTo = this.accountsRepository.getAccount(accountToId);

    synchronized (this){
      if(accountFrom.getBalance().compareTo(amount) < 0){
        throw new IllegalArgumentException("Insufficient balance in accountFrom");
      }

      // Perform the money transfer
      accountFrom.withdraw(amount);
      accountTo.deposite(amount);
    }

    // Update the accounts
    this.accountsRepository.updateAccount(accountFrom);
    this.accountsRepository.updateAccount(accountTo);

    // Notify both account holders
    emailNotificationService.notifyAboutTransfer(accountFrom , "Amount transferred : " + amount + " to account " + accountTo.getAccountId());
    emailNotificationService.notifyAboutTransfer(accountTo , "Amount transferred : " + amount + " from account " + accountFrom.getAccountId());
  }
}
