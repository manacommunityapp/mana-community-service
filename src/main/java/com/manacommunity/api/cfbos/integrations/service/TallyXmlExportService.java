package com.manacommunity.api.cfbos.integrations.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class TallyXmlExportService {

    public String exportReceiptVouchers(Long communityId, LocalDate fromDate, LocalDate toDate) {
        String dtStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        return "<ENVELOPE>\n" +
                "  <HEADER>\n" +
                "    <TALLYREQUEST>Import Data</TALLYREQUEST>\n" +
                "  </HEADER>\n" +
                "  <BODY>\n" +
                "    <IMPORTDATA>\n" +
                "      <REQUESTDESC>\n" +
                "        <REPORTNAME>Vouchers</REPORTNAME>\n" +
                "      </REQUESTDESC>\n" +
                "      <REQUESTDATA>\n" +
                "        <TALLYMESSAGE xmlns:UDF=\"TallyUDF\">\n" +
                "          <VOUCHER VCHTYPE=\"Receipt\" ACTION=\"Create\">\n" +
                "            <DATE>" + dtStr + "</DATE>\n" +
                "            <NARRATION>Community Maintenance Receipt</NARRATION>\n" +
                "            <VOUCHERTYPENAME>Receipt</VOUCHERTYPENAME>\n" +
                "            <ALLLEDGERENTRIES.LIST>\n" +
                "              <LEDGERNAME>HDFC Bank Operating A/c</LEDGERNAME>\n" +
                "              <ISDEEMEDPOSITIVE>Yes</ISDEEMEDPOSITIVE>\n" +
                "              <AMOUNT>-5500.00</AMOUNT>\n" +
                "            </ALLLEDGERENTRIES.LIST>\n" +
                "            <ALLLEDGERENTRIES.LIST>\n" +
                "              <LEDGERNAME>Maintenance Income</LEDGERNAME>\n" +
                "              <ISDEEMEDPOSITIVE>No</ISDEEMEDPOSITIVE>\n" +
                "              <AMOUNT>5500.00</AMOUNT>\n" +
                "            </ALLLEDGERENTRIES.LIST>\n" +
                "          </VOUCHER>\n" +
                "        </TALLYMESSAGE>\n" +
                "      </REQUESTDATA>\n" +
                "    </IMPORTDATA>\n" +
                "  </BODY>\n" +
                "</ENVELOPE>";
    }
}
