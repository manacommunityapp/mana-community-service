package com.manacommunity.api.cfbos.accounting.service;

import com.manacommunity.api.cfbos.accounting.dto.AccountDto;
import com.manacommunity.api.cfbos.accounting.dto.AccountTreeNodeDto;
import com.manacommunity.api.cfbos.accounting.entity.Account;
import com.manacommunity.api.cfbos.accounting.repository.AccountRepository;
import com.manacommunity.api.cfbos.shared.exception.CfbosResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChartOfAccountsService {

    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public List<AccountTreeNodeDto> getAccountTree() {
        List<Account> roots = accountRepository.findByParentAccountIsNullAndIsActiveTrue();
        return roots.stream().map(this::buildTreeNode).toList();
    }

    @Transactional(readOnly = true)
    public AccountDto getAccount(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new CfbosResourceNotFoundException("Account", id));
        return toDto(account);
    }

    @Transactional(readOnly = true)
    public Account findByCode(String code) {
        return accountRepository.findByCode(code)
                .orElseThrow(() -> new CfbosResourceNotFoundException("Account " + code, 0L));
    }

    @Transactional
    public AccountDto createAccount(AccountDto dto) {
        Account parent = dto.getParentAccountId() != null
                ? accountRepository.findById(dto.getParentAccountId()).orElse(null)
                : null;

        Account account = Account.builder()
                .code(dto.getCode())
                .name(dto.getName())
                .accountType(dto.getAccountType())
                .parentAccount(parent)
                .isSystemAccount(dto.getIsSystemAccount() != null ? dto.getIsSystemAccount() : false)
                .isBankAccount(dto.getIsBankAccount() != null ? dto.getIsBankAccount() : false)
                .description(dto.getDescription())
                .build();

        if (parent != null) {
            account.setAccountGroup(parent.getAccountGroup());
        }

        return toDto(accountRepository.save(account));
    }

    private AccountTreeNodeDto buildTreeNode(Account account) {
        List<Account> children = accountRepository.findByParentAccountAndIsActiveTrue(account);
        return AccountTreeNodeDto.builder()
                .id(account.getId())
                .code(account.getCode())
                .name(account.getName())
                .accountType(account.getAccountType())
                .currentBalance(account.getCurrentBalance())
                .isSystemAccount(account.getIsSystemAccount())
                .children(children.stream().map(this::buildTreeNode).toList())
                .build();
    }

    private AccountDto toDto(Account e) {
        return AccountDto.builder()
                .id(e.getId()).code(e.getCode()).name(e.getName())
                .accountType(e.getAccountType())
                .accountGroupName(e.getAccountGroup() != null ? e.getAccountGroup().getName() : null)
                .parentAccountId(e.getParentAccount() != null ? e.getParentAccount().getId() : null)
                .isSystemAccount(e.getIsSystemAccount())
                .isBankAccount(e.getIsBankAccount())
                .isActive(e.getIsActive())
                .openingBalance(e.getOpeningBalance())
                .currentBalance(e.getCurrentBalance())
                .description(e.getDescription())
                .build();
    }
}
