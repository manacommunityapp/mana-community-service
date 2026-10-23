package com.manacommunity.api.cfbos;

import com.manacommunity.api.cfbos.treasury.entity.BankStatementTransaction;
import com.manacommunity.api.cfbos.treasury.parser.Camt053BankStatementParser;
import com.manacommunity.api.cfbos.treasury.parser.Mt940BankStatementParser;
import com.manacommunity.api.model.Community;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BankStatementParserTest {

    private Mt940BankStatementParser mt940Parser;
    private Camt053BankStatementParser camt053Parser;
    private Community sampleCommunity;

    @BeforeEach
    void setUp() {
        mt940Parser = new Mt940BankStatementParser();
        camt053Parser = new Camt053BankStatementParser();
        sampleCommunity = Community.builder().id(1L).name("Mana Capital").build();
    }

    @Test
    @DisplayName("MT940: Parses SWIFT statement lines with CR/DR and detects flat number")
    void testMt940Parsing() {
        String mt940Data = ":20:STARTMR\n" +
                ":25:123456789\n" +
                ":28C:00001\n" +
                ":60F:C260101INR100000,00\n" +
                ":61:2610011001C5500,00NTRFNONREF//UTR987654321\n" +
                ":86:NEFT-INWARD-FLAT A-1204 MAINTENANCE OCTOBER\n" +
                ":62F:C261001INR105500,00";

        List<BankStatementTransaction> list = mt940Parser.parse(mt940Data, sampleCommunity);
        assertThat(list).hasSize(1);

        BankStatementTransaction tx = list.get(0);
        assertThat(tx.getEntryType()).isEqualTo("CR");
        assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("5500.00"));
        assertThat(tx.getDetectedFlatNumber()).isEqualTo("A-1204");
    }

    @Test
    @DisplayName("CAMT.053: Parses ISO 20022 XML statement lines and extracts flat number")
    void testCamt053Parsing() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:camt.053.001.02\">\n" +
                "  <BkToCstmrStmt>\n" +
                "    <Stmt>\n" +
                "      <Id>STMT-2026-001</Id>\n" +
                "      <Ntry>\n" +
                "        <Amt Ccy=\"INR\">8200.00</Amt>\n" +
                "        <CdtDbtInd>CRDT</CdtDbtInd>\n" +
                "        <BookgDt><Dt>2026-10-01</Dt></BookgDt>\n" +
                "        <NtryDtls>\n" +
                "          <TxDtls>\n" +
                "            <Refs><EndToEndId>UPI-TXN-112233</EndToEndId></Refs>\n" +
                "            <RmtInf><Ustrd>UPI Transfer from resident B-302 for maintenance</Ustrd></RmtInf>\n" +
                "          </TxDtls>\n" +
                "        </NtryDtls>\n" +
                "      </Ntry>\n" +
                "    </Stmt>\n" +
                "  </BkToCstmrStmt>\n" +
                "</Document>";

        List<BankStatementTransaction> list = camt053Parser.parse(xml, sampleCommunity);
        assertThat(list).hasSize(1);

        BankStatementTransaction tx = list.get(0);
        assertThat(tx.getEntryType()).isEqualTo("CR");
        assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("8200.00"));
        assertThat(tx.getDetectedFlatNumber()).isEqualTo("B-302");
    }
}
