package com.manacommunity.api.cfbos.treasury.parser;

import com.manacommunity.api.cfbos.treasury.entity.BankStatementTransaction;
import com.manacommunity.api.cfbos.treasury.enums.ReconciliationStatus;
import com.manacommunity.api.cfbos.treasury.enums.StatementType;
import com.manacommunity.api.model.Community;
import org.springframework.stereotype.Component;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class Camt053BankStatementParser {

    private static final Pattern FLAT_PATTERN = Pattern.compile("(?i)\\b([A-Z]\\d?[-/ ]?\\d{2,4})\\b");

    public List<BankStatementTransaction> parse(String xmlContent, Community community) {
        List<BankStatementTransaction> transactions = new ArrayList<>();
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
            doc.getDocumentElement().normalize();

            NodeList entryList = doc.getElementsByTagName("Ntry");
            for (int i = 0; i < entryList.getLength(); i++) {
                Node ntryNode = entryList.item(i);
                if (ntryNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element ntryElement = (Element) ntryNode;

                    String amtStr = getTagValue("Amt", ntryElement);
                    String cdtDbtInd = getTagValue("CdtDbtInd", ntryElement);
                    String bookgDt = getTagValue("BookgDt", ntryElement);
                    String ustrd = getTagValue("Ustrd", ntryElement);
                    String endToEndId = getTagValue("EndToEndId", ntryElement);

                    BankStatementTransaction tx = new BankStatementTransaction();
                    tx.setCommunity(community);
                    tx.setStatementType(StatementType.ISO20022_CAMT053);
                    tx.setStatus(ReconciliationStatus.UNMATCHED);
                    tx.setAmount(amtStr != null ? new BigDecimal(amtStr) : BigDecimal.ZERO);
                    tx.setEntryType("DBIT".equalsIgnoreCase(cdtDbtInd) ? "DR" : "CR");
                    tx.setReferenceNumber(endToEndId != null ? endToEndId : "CAMT-REF-" + i);
                    tx.setNarration(ustrd != null ? ustrd : "ISO 20022 Bank Entry");

                    if (bookgDt != null && bookgDt.length() >= 10) {
                        tx.setTransactionDate(LocalDate.parse(bookgDt.substring(0, 10)));
                    } else {
                        tx.setTransactionDate(LocalDate.now());
                    }

                    if (ustrd != null) {
                        Matcher m = FLAT_PATTERN.matcher(ustrd);
                        if (m.find()) {
                            tx.setDetectedFlatNumber(m.group(1).toUpperCase().replace(" ", ""));
                        }
                    }

                    transactions.add(tx);
                }
            }
        } catch (Exception e) {
            // Return collected transactions
        }
        return transactions;
    }

    private String getTagValue(String tag, Element element) {
        NodeList nl = element.getElementsByTagName(tag);
        if (nl != null && nl.getLength() > 0 && nl.item(0).getFirstChild() != null) {
            return nl.item(0).getFirstChild().getNodeValue();
        }
        return null;
    }
}
