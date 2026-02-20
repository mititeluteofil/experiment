package com.example.demo.service;

import com.example.demo.dto.TransferRequest;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.InsufficientFundsException;
import com.example.demo.model.BankAccount;
import com.example.demo.model.Transaction;
import com.example.demo.model.TransactionStatus;
import com.example.demo.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final BankAccountService bankAccountService;
    private final TransactionRepository transactionRepository;

    /**
     * Deliberately naive transfer — no locking, no idempotency key.
     * Two concurrent transfers from the same account can both pass the balance check
     * and cause an overdraft (lost update / race condition).
     */
    @Transactional
    public Transaction transfer(TransferRequest request, String userEmail) {
        if (request.fromAccountId().equals(request.toAccountId())) {
            throw new BadRequestException("Cannot transfer to the same account");
        }

        BankAccount from = bankAccountService.getById(request.fromAccountId());
        BankAccount to = bankAccountService.getById(request.toAccountId());

        // Verify the sender owns the source account
        if (!from.getUser().getEmail().equals(userEmail)) {
            throw new BadRequestException("You do not own the source account");
        }

        if (!from.getCurrency().equals(to.getCurrency())) {
            throw new BadRequestException(
                    "Currency mismatch: " + from.getCurrency() + " vs " + to.getCurrency());
        }

        if (from.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }

        // Naive read-modify-write — race condition lives here
        from.setBalance(from.getBalance().subtract(request.amount()));
        to.setBalance(to.getBalance().add(request.amount()));

        Transaction tx = new Transaction();
        tx.setFromAccount(from);
        tx.setToAccount(to);
        tx.setAmount(request.amount());
        tx.setCurrency(from.getCurrency());
        tx.setStatus(TransactionStatus.COMPLETED);
        tx.setDescription(request.description());

        return transactionRepository.save(tx);
    }
}
