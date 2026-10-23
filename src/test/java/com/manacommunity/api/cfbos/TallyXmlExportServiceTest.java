package com.manacommunity.api.cfbos;

import com.manacommunity.api.cfbos.integrations.service.TallyXmlExportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class TallyXmlExportServiceTest {

    private final TallyXmlExportService service = new TallyXmlExportService();

    @Test
    @DisplayName("Tally XML: Generates valid standard Tally import XML voucher envelope")
    void testTallyXmlExport() {
        String xml = service.exportReceiptVouchers(1L, LocalDate.now().minusMonths(1), LocalDate.now());
        assertThat(xml).contains("<ENVELOPE>");
        assertThat(xml).contains("<TALLYREQUEST>Import Data</TALLYREQUEST>");
        assertThat(xml).contains("<VOUCHER VCHTYPE=\"Receipt\"");
        assertThat(xml).contains("<LEDGERNAME>HDFC Bank Operating A/c</LEDGERNAME>");
    }
}
