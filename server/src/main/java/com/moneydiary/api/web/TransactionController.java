package com.moneydiary.api.web;

import com.moneydiary.api.domain.Transaction;
import com.moneydiary.api.repository.TransactionRepository;
import com.moneydiary.api.security.FirebaseUser;
import com.moneydiary.api.web.dto.TransactionRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionRepository repository;

    public TransactionController(TransactionRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Transaction> list(@AuthenticationPrincipal FirebaseUser user) {
        return repository.findAll(user.uid());
    }

    @GetMapping("/{id}")
    public Transaction get(@AuthenticationPrincipal FirebaseUser user, @PathVariable String id) {
        return repository.findById(user.uid(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Transaction create(@AuthenticationPrincipal FirebaseUser user,
                              @RequestBody TransactionRequest request) {
        Transaction transaction = toEntity(request);
        transaction.setCreatedAt(System.currentTimeMillis());
        return repository.create(user.uid(), transaction);
    }

    @PutMapping("/{id}")
    public Transaction update(@AuthenticationPrincipal FirebaseUser user,
                              @PathVariable String id,
                              @RequestBody TransactionRequest request) {
        Transaction existing = repository.findById(user.uid(), id);
        Transaction updated = toEntity(request);
        updated.setCreatedAt(existing.getCreatedAt());
        if (updated.getClientId() == null) {
            updated.setClientId(existing.getClientId());
        }
        return repository.update(user.uid(), id, updated);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal FirebaseUser user, @PathVariable String id) {
        repository.delete(user.uid(), id);
    }

    private Transaction toEntity(TransactionRequest request) {
        if (request.type() == null) {
            throw new IllegalArgumentException("type 은 필수입니다 (INCOME 또는 EXPENSE).");
        }
        if (request.amount() == null || request.amount() < 0) {
            throw new IllegalArgumentException("amount 는 0 이상의 값이어야 합니다.");
        }
        if (request.category() == null || request.category().isBlank()) {
            throw new IllegalArgumentException("category 는 필수입니다.");
        }
        validateDate(request.date());

        Transaction transaction = new Transaction();
        transaction.setType(request.type());
        transaction.setAmount(request.amount());
        transaction.setCategory(request.category());
        transaction.setDate(request.date());
        transaction.setMemo(request.memo());
        transaction.setClientId(request.clientId());
        return transaction;
    }

    private void validateDate(String date) {
        if (date == null || date.isBlank()) {
            throw new IllegalArgumentException("date 는 필수입니다 (yyyy-MM-dd).");
        }
        try {
            LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("date 형식이 올바르지 않습니다 (yyyy-MM-dd): " + date);
        }
    }
}
