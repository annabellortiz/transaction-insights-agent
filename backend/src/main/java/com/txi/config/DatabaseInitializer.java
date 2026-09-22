package com.txi.config;

import com.txi.db.entity.Account;
import com.txi.db.repository.AccountRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer {

    private final AccountRepository accountRepository;

    public DatabaseInitializer(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @PostConstruct
    public void ensureDefaultAccount() {
        if (accountRepository.count() == 0) {
            Account a = new Account("Default Account");
            a.setExternalId("default-1");
            accountRepository.save(a);
        }
    }
}
