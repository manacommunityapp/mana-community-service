package com.manacommunity.api.cfbos.treasury.service;

import com.manacommunity.api.cfbos.treasury.dto.FundAccountDto;
import com.manacommunity.api.cfbos.treasury.dto.FundExpenditureRequest;
import com.manacommunity.api.cfbos.treasury.entity.FundAccount;
import com.manacommunity.api.cfbos.treasury.entity.FundContribution;
import com.manacommunity.api.cfbos.treasury.entity.FundExpenditure;
import com.manacommunity.api.cfbos.treasury.enums.ContributionStatus;
import com.manacommunity.api.cfbos.treasury.enums.FundType;
import com.manacommunity.api.cfbos.treasury.repository.FundAccountRepository;
import com.manacommunity.api.cfbos.treasury.repository.FundContributionRepository;
import com.manacommunity.api.cfbos.treasury.repository.FundExpenditureRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TreasuryFundService {

    private final FundAccountRepository fundAccountRepository;
    private final FundContributionRepository contributionRepository;
    private final FundExpenditureRepository expenditureRepository;

    public List<FundAccountDto> getFundAccounts(Long communityId) {
        return fundAccountRepository.findByCommunityId(communityId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public FundAccountDto createFundAccount(FundAccountDto dto, Community community) {
        FundAccount account = FundAccount.builder()
                .community(community)
                .name(dto.getName())
                .fundType(dto.getFundType())
                .targetAmount(dto.getTargetAmount() != null ? dto.getTargetAmount() : BigDecimal.ZERO)
                .currentBalance(dto.getCurrentBalance() != null ? dto.getCurrentBalance() : BigDecimal.ZERO)
                .minimumReserveFloor(dto.getMinimumReserveFloor() != null ? dto.getMinimumReserveFloor() : BigDecimal.ZERO)
                .bankAccountNumber(dto.getBankAccountNumber())
                .bankName(dto.getBankName())
                .description(dto.getDescription())
                .active(true)
                .build();

        return mapToDto(fundAccountRepository.save(account));
    }

    @Transactional
    public FundContribution recordContribution(Long fundAccountId, String flatNumber, BigDecimal amount, AppUser resident, String txRef) {
        FundAccount account = fundAccountRepository.findById(fundAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Fund account not found: " + fundAccountId));

        FundContribution contribution = FundContribution.builder()
                .fundAccount(account)
                .flatNumber(flatNumber)
                .resident(resident)
                .amount(amount)
                .transactionRef(txRef)
                .status(ContributionStatus.CREDITED)
                .build();

        account.setCurrentBalance(account.getCurrentBalance().add(amount));
        fundAccountRepository.save(account);

        return contributionRepository.save(contribution);
    }

    @Transactional
    public FundExpenditure recordExpenditure(FundExpenditureRequest req, AppUser authorizer) {
        FundAccount account = fundAccountRepository.findById(req.getFundAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Fund account not found: " + req.getFundAccountId()));

        if (account.getCurrentBalance().subtract(req.getAmount()).compareTo(account.getMinimumReserveFloor()) < 0) {
            throw new IllegalStateException("Expenditure breaches minimum reserve floor of " + account.getMinimumReserveFloor());
        }

        FundExpenditure exp = FundExpenditure.builder()
                .fundAccount(account)
                .title(req.getTitle())
                .description(req.getDescription())
                .amount(req.getAmount())
                .approvedByResolutionNo(req.getApprovedByResolutionNo())
                .vendorId(req.getVendorId())
                .vendorName(req.getVendorName())
                .invoiceNumber(req.getInvoiceNumber())
                .quotationAttachmentUrl(req.getQuotationAttachmentUrl())
                .approvedBy(authorizer)
                .build();

        account.setCurrentBalance(account.getCurrentBalance().subtract(req.getAmount()));
        fundAccountRepository.save(account);

        return expenditureRepository.save(exp);
    }

    private FundAccountDto mapToDto(FundAccount a) {
        return FundAccountDto.builder()
                .id(a.getId())
                .name(a.getName())
                .fundType(a.getFundType())
                .currentBalance(a.getCurrentBalance())
                .targetAmount(a.getTargetAmount())
                .minimumReserveFloor(a.getMinimumReserveFloor())
                .bankAccountNumber(a.getBankAccountNumber())
                .bankName(a.getBankName())
                .description(a.getDescription())
                .active(a.getActive())
                .build();
    }
}
