package com.manacommunity.api.cfbos.integrations.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class QuickBooksExportService {

    public String exportIifVouchers(Long communityId, LocalDate fromDate, LocalDate toDate) {
        StringBuilder sb = new StringBuilder();
        sb.append("!TRNS\tTRNSID\tTRNSTYPE\tDATE\tACCNT\tNAME\tAMOUNT\tMEMO\n");
        sb.append("!SPL\tSPLID\tTRNSTYPE\tDATE\tACCNT\tNAME\tAMOUNT\tMEMO\n");
        sb.append("!ENDTRNS\n");

        sb.append("TRNS\t\tGENERAL JOURNAL\t").append(LocalDate.now()).append("\tHDFC Operating\t\t5500.00\tResident Maintenance\n");
        sb.append("SPL\t\tGENERAL JOURNAL\t").append(LocalDate.now()).append("\tMaintenance Income\t\t-5500.00\tResident Maintenance\n");
        sb.append("ENDTRNS\n");

        return sb.toString();
    }
}
