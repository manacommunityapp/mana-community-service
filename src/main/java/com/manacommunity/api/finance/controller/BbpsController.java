package com.manacommunity.api.finance.controller;

import com.manacommunity.api.finance.entity.BbpsBillFetch;
import com.manacommunity.api.finance.repository.BbpsBillFetchRepository;
import com.manacommunity.api.payments.dto.CreatePaymentOrderRequest;
import com.manacommunity.api.payments.entity.PaymentOrder;
import com.manacommunity.api.payments.model.PaymentGateway;
import com.manacommunity.api.payments.model.PaymentOrderStatus;
import com.manacommunity.api.payments.model.PaymentReferenceType;
import com.manacommunity.api.payments.service.UnifiedPaymentOrderService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * BBPS (Bharat Bill Payment System) stub controller.
 *
 * Exposes 3 endpoints that the mobile billerService.ts calls:
 *   GET  /api/v1/bbps/billers?category=ELECTRICITY
 *   POST /api/v1/bbps/bill/fetch
 *   POST /api/v1/bbps/bill/pay
 *
 * All responses are deterministic mocks. Wire to Setu or Razorpay
 * BillPay once BBPS aggregator credentials are provisioned.
 *
 * Disabled in production profile (!prod) until a licensed aggregator is provisioned.
 */
@Slf4j
@Profile("!prod")
@RestController
@RequestMapping("/api/v1/bbps")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class BbpsController {

    private final LoggedInUserService          loggedInUserService;
    private final UnifiedPaymentOrderService   paymentOrderService;
    private final BbpsBillFetchRepository      billFetchRepository;

    // ── Inner DTOs ────────────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BillerDto {
        private String id;
        private String name;
        private String shortName;
        private String category;
        private String icon;
        private String state;
        private String consumerIdLabel;
        private String consumerIdHint;
        private Integer consumerIdLength;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class BillFetchRequest {
        private String billerId;
        private String consumerId;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class FetchedBillDto {
        private String billerId;
        private String billerName;
        private String consumerId;
        private String customerName;
        private String billNumber;
        private String billDate;
        private String dueDate;
        private BigDecimal amount;
        private BigDecimal overdueAmount;
        private Integer units;
        private String periodFrom;
        private String periodTo;
        private String status; // DUE | OVERDUE | PAID
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class BillPayRequest {
        private String billNumber;
        private String billerId;
        private BigDecimal amount;
        private String method; // UPI | WALLET
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BillPaymentResponse {
        private String upiIntentUrl;
        private String phonePeUrl;
        private String transactionRef;
        private Long paymentOrderId;
        private BigDecimal amount;
    }

    // ── Static biller catalogue ───────────────────────────────────

    private static final List<BillerDto> ELECTRICITY_BILLERS = List.of(
        BillerDto.builder().id("BESCOM").name("BESCOM – Bangalore Electricity Supply Company")
            .shortName("BESCOM").category("ELECTRICITY").icon("⚡").state("Karnataka")
            .consumerIdLabel("RR Number (Consumer Account No.)").consumerIdHint("e.g. 9123456789")
            .consumerIdLength(10).build(),
        BillerDto.builder().id("TSSPDCL").name("TSSPDCL – Telangana Southern Power Distribution")
            .shortName("TSSPDCL").category("ELECTRICITY").icon("⚡").state("Telangana")
            .consumerIdLabel("Service Number (CA Number)").consumerIdHint("e.g. 0012345678")
            .consumerIdLength(10).build(),
        BillerDto.builder().id("TATA_POWER_MUMBAI").name("Tata Power – Mumbai Distribution")
            .shortName("Tata Power").category("ELECTRICITY").icon("⚡").state("Maharashtra")
            .consumerIdLabel("Consumer Number").consumerIdHint("e.g. 4001234567")
            .consumerIdLength(10).build(),
        BillerDto.builder().id("ADANI_ELECTRICITY").name("Adani Electricity – Mumbai")
            .shortName("Adani").category("ELECTRICITY").icon("⚡").state("Maharashtra")
            .consumerIdLabel("Consumer Account Number").consumerIdHint("e.g. 100023456").build(),
        BillerDto.builder().id("MSEDCL").name("MSEDCL – Maharashtra State Electricity")
            .shortName("MSEDCL").category("ELECTRICITY").icon("⚡").state("Maharashtra")
            .consumerIdLabel("Consumer Number").consumerIdHint("e.g. 020012345678").build(),
        BillerDto.builder().id("TNEB").name("TNEB – Tamil Nadu Electricity Board")
            .shortName("TNEB").category("ELECTRICITY").icon("⚡").state("Tamil Nadu")
            .consumerIdLabel("Consumer Number (EB No.)").consumerIdHint("e.g. 001234567890").build(),
        BillerDto.builder().id("BSES_RAJDHANI").name("BSES Rajdhani – South & West Delhi")
            .shortName("BSES Rajdhani").category("ELECTRICITY").icon("⚡").state("Delhi")
            .consumerIdLabel("Consumer Account Number (CA)").consumerIdHint("e.g. 50012345678").build(),
        BillerDto.builder().id("BSES_YAMUNA").name("BSES Yamuna – East & Central Delhi")
            .shortName("BSES Yamuna").category("ELECTRICITY").icon("⚡").state("Delhi")
            .consumerIdLabel("Consumer Account Number (CA)").consumerIdHint("e.g. 30012345678").build()
    );

    private static final List<BillerDto> GAS_BILLERS = List.of(
        BillerDto.builder().id("MGL").name("MGL – Mahanagar Gas Limited").shortName("MGL").category("GAS").icon("🔥").state("Maharashtra").consumerIdLabel("Business Partner (BP) Number").consumerIdHint("e.g. 200012345").build(),
        BillerDto.builder().id("IGL").name("IGL – Indraprastha Gas Limited").shortName("IGL").category("GAS").icon("🔥").state("Delhi").consumerIdLabel("BP Number").consumerIdHint("e.g. 100009876").build()
    );

    private static final List<BillerDto> WATER_BILLERS = List.of(
        BillerDto.builder().id("BWSSB").name("BWSSB – Bangalore Water Supply").shortName("BWSSB").category("WATER").icon("💧").state("Karnataka").consumerIdLabel("Consumer ID").consumerIdHint("e.g. 1234567890").consumerIdLength(10).build(),
        BillerDto.builder().id("MCGM_WATER").name("MCGM – Mumbai Water Supply").shortName("MCGM").category("WATER").icon("💧").state("Maharashtra").consumerIdLabel("Consumer Number").consumerIdHint("e.g. 010123456789").build(),
        BillerDto.builder().id("HMWSSB").name("HMWSSB – Hyderabad Metro Water").shortName("HMWSSB").category("WATER").icon("💧").state("Telangana").consumerIdLabel("Service Connection No.").consumerIdHint("e.g. HYD2012345").build(),
        BillerDto.builder().id("DJB").name("DJB – Delhi Jal Board").shortName("DJB").category("WATER").icon("💧").state("Delhi").consumerIdLabel("K Number").consumerIdHint("e.g. K-1234567").build(),
        BillerDto.builder().id("CMWSSB").name("CMWSSB – Chennai Metro Water").shortName("CMWSSB").category("WATER").icon("💧").state("Tamil Nadu").consumerIdLabel("Consumer ID").consumerIdHint("e.g. 12345678").build()
    );

    private static final List<BillerDto> BROADBAND_BILLERS = List.of(
        BillerDto.builder().id("JIO_FIBER").name("Jio Fiber – Reliance Jio").shortName("Jio Fiber").category("BROADBAND").icon("📡").state("All India").consumerIdLabel("Account Number / Mobile").consumerIdHint("e.g. 10-digit Jio number").build(),
        BillerDto.builder().id("AIRTEL_BROADBAND").name("Airtel Xstream Fiber").shortName("Airtel").category("BROADBAND").icon("📡").state("All India").consumerIdLabel("Airtel Account Number").consumerIdHint("e.g. 123456789012").build(),
        BillerDto.builder().id("ACT_BROADBAND").name("ACT Fibernet").shortName("ACT").category("BROADBAND").icon("📡").state("Multi").consumerIdLabel("Subscriber ID").consumerIdHint("e.g. ACT12345678").build(),
        BillerDto.builder().id("BSNL_BROADBAND").name("BSNL Bharat Fiber / FTTH").shortName("BSNL").category("BROADBAND").icon("📡").state("All India").consumerIdLabel("CA Number / Landline Number").consumerIdHint("e.g. 01112345678").build()
    );

    private static final List<BillerDto> DTH_BILLERS = List.of(
        BillerDto.builder().id("TATA_PLAY").name("Tata Play (formerly Tata Sky)").shortName("Tata Play").category("DTH").icon("📺").state("All India").consumerIdLabel("Subscriber ID (RM ID)").consumerIdHint("e.g. 1234567890").consumerIdLength(10).build(),
        BillerDto.builder().id("DISH_TV").name("Dish TV").shortName("Dish TV").category("DTH").icon("📺").state("All India").consumerIdLabel("VC Number").consumerIdHint("e.g. 01234567890").consumerIdLength(11).build(),
        BillerDto.builder().id("AIRTEL_DTH").name("Airtel Digital TV").shortName("Airtel DTH").category("DTH").icon("📺").state("All India").consumerIdLabel("Customer ID").consumerIdHint("e.g. 3012345678").consumerIdLength(10).build(),
        BillerDto.builder().id("SUN_DIRECT").name("Sun Direct DTH").shortName("Sun Direct").category("DTH").icon("📺").state("South").consumerIdLabel("Smart Card Number").consumerIdHint("e.g. 1234567890123").consumerIdLength(13).build(),
        BillerDto.builder().id("D2H").name("D2H (Videocon D2H)").shortName("D2H").category("DTH").icon("📺").state("All India").consumerIdLabel("Customer ID").consumerIdHint("e.g. 123456789").build()
    );

    private static final List<BillerDto> FASTAG_BILLERS = List.of(
        BillerDto.builder().id("NHAI_ONE").name("NHAI – National Highways Authority (One FASTag)").shortName("NHAI").category("FASTAG").icon("🚗").state("All India").consumerIdLabel("Vehicle Registration Number").consumerIdHint("e.g. KA01AB1234").build(),
        BillerDto.builder().id("HDFC_FASTAG").name("HDFC Bank FASTag").shortName("HDFC FASTag").category("FASTAG").icon("🚗").state("All India").consumerIdLabel("Vehicle Registration Number").consumerIdHint("e.g. MH02CD5678").build(),
        BillerDto.builder().id("SBI_FASTAG").name("SBI FASTag – State Bank of India").shortName("SBI FASTag").category("FASTAG").icon("🚗").state("All India").consumerIdLabel("Vehicle Registration Number").consumerIdHint("e.g. DL03EF9012").build(),
        BillerDto.builder().id("ICICI_FASTAG").name("ICICI Bank FASTag").shortName("ICICI FASTag").category("FASTAG").icon("🚗").state("All India").consumerIdLabel("Vehicle Registration Number").consumerIdHint("e.g. TN04GH3456").build(),
        BillerDto.builder().id("PAYTM_FASTAG").name("Paytm Payments Bank FASTag").shortName("Paytm FASTag").category("FASTAG").icon("🚗").state("All India").consumerIdLabel("Vehicle Registration Number").consumerIdHint("e.g. KA05IJ7890").build(),
        BillerDto.builder().id("AXIS_FASTAG").name("Axis Bank FASTag").shortName("Axis FASTag").category("FASTAG").icon("🚗").state("All India").consumerIdLabel("Vehicle Registration Number").consumerIdHint("e.g. GJ06KL2345").build()
    );

    private static final List<BillerDto> MUNICIPAL_TAX_BILLERS = List.of(
        BillerDto.builder().id("GHMC").name("GHMC – Greater Hyderabad Municipal Corporation").shortName("GHMC").category("MUNICIPAL_TAX").icon("🏛").state("Telangana").consumerIdLabel("PTIN (Property Tax ID No.)").consumerIdHint("e.g. 123456789").build(),
        BillerDto.builder().id("BBMP").name("BBMP – Bruhat Bengaluru Mahanagara Palike").shortName("BBMP").category("MUNICIPAL_TAX").icon("🏛").state("Karnataka").consumerIdLabel("PID Number / Application ID").consumerIdHint("e.g. 1234567890").build(),
        BillerDto.builder().id("BMC").name("BMC – Brihanmumbai Municipal Corporation").shortName("BMC").category("MUNICIPAL_TAX").icon("🏛").state("Maharashtra").consumerIdLabel("Property Account Number").consumerIdHint("e.g. 0123456789").build(),
        BillerDto.builder().id("MCD").name("MCD – Municipal Corporation of Delhi").shortName("MCD").category("MUNICIPAL_TAX").icon("🏛").state("Delhi").consumerIdLabel("Property ID / UPIC No.").consumerIdHint("e.g. D0123456").build(),
        BillerDto.builder().id("PCMC").name("PCMC – Pimpri-Chinchwad Municipal Corporation").shortName("PCMC").category("MUNICIPAL_TAX").icon("🏛").state("Maharashtra").consumerIdLabel("Property Number").consumerIdHint("e.g. PCM/1234/567").build(),
        BillerDto.builder().id("NMMC").name("NMMC – Navi Mumbai Municipal Corporation").shortName("NMMC").category("MUNICIPAL_TAX").icon("🏛").state("Maharashtra").consumerIdLabel("Assessment Number").consumerIdHint("e.g. 12345678").build(),
        BillerDto.builder().id("CMC_CHENNAI").name("Greater Chennai Corporation").shortName("Chennai Corp").category("MUNICIPAL_TAX").icon("🏛").state("Tamil Nadu").consumerIdLabel("Zone-Division-Bill No.").consumerIdHint("e.g. 1-2-12345678").build()
    );

    private static final List<BillerDto> CREDIT_CARD_BILLERS = List.of(
        BillerDto.builder().id("HDFC_CC").name("HDFC Bank Credit Card").shortName("HDFC").category("CREDIT_CARD").icon("💳").state("All India").consumerIdLabel("Credit Card Number (16 digits)").consumerIdHint("e.g. 4111 1111 1111 1111").consumerIdLength(16).build(),
        BillerDto.builder().id("SBI_CC").name("SBI Card – State Bank Credit").shortName("SBI Card").category("CREDIT_CARD").icon("💳").state("All India").consumerIdLabel("Credit Card Number (16 digits)").consumerIdHint("e.g. 5425 2334 3010 9903").consumerIdLength(16).build(),
        BillerDto.builder().id("ICICI_CC").name("ICICI Bank Credit Card").shortName("ICICI").category("CREDIT_CARD").icon("💳").state("All India").consumerIdLabel("Credit Card Number (16 digits)").consumerIdHint("e.g. 4000 0566 5566 5556").consumerIdLength(16).build(),
        BillerDto.builder().id("AXIS_CC").name("Axis Bank Credit Card").shortName("Axis").category("CREDIT_CARD").icon("💳").state("All India").consumerIdLabel("Credit Card Number (16 digits)").consumerIdHint("e.g. 5425 2334 3010 9903").consumerIdLength(16).build(),
        BillerDto.builder().id("KOTAK_CC").name("Kotak Mahindra Bank Credit Card").shortName("Kotak").category("CREDIT_CARD").icon("💳").state("All India").consumerIdLabel("Credit Card Number (16 digits)").consumerIdHint("e.g. 4111 1111 1111 1111").consumerIdLength(16).build(),
        BillerDto.builder().id("AMEX_CC").name("American Express India").shortName("Amex").category("CREDIT_CARD").icon("💳").state("All India").consumerIdLabel("Card Number (15 digits)").consumerIdHint("e.g. 3714 496353 98431").consumerIdLength(15).build(),
        BillerDto.builder().id("INDUSIND_CC").name("IndusInd Bank Credit Card").shortName("IndusInd").category("CREDIT_CARD").icon("💳").state("All India").consumerIdLabel("Credit Card Number (16 digits)").consumerIdHint("e.g. 4111 1111 1111 1111").consumerIdLength(16).build()
    );

    private static final List<BillerDto> EDUCATION_BILLERS = List.of(
        BillerDto.builder().id("CBSE").name("CBSE – Central Board of Secondary Education").shortName("CBSE").category("EDUCATION").icon("🎓").state("All India").consumerIdLabel("Application / Roll Number").consumerIdHint("e.g. 3012345678").build(),
        BillerDto.builder().id("DU").name("Delhi University Fee Portal").shortName("DU").category("EDUCATION").icon("🎓").state("Delhi").consumerIdLabel("Enrollment Number").consumerIdHint("e.g. DU2024001234").build(),
        BillerDto.builder().id("BITS").name("BITS Pilani – Fee Payment").shortName("BITS Pilani").category("EDUCATION").icon("🎓").state("Rajasthan").consumerIdLabel("Student ID (BITS ID)").consumerIdHint("e.g. 2024A7PS1234G").build(),
        BillerDto.builder().id("AMITY").name("Amity University Fee Portal").shortName("Amity").category("EDUCATION").icon("🎓").state("Multi").consumerIdLabel("Enrollment Number").consumerIdHint("e.g. A2024123456").build(),
        BillerDto.builder().id("VTU").name("VTU – Visvesvaraya Technological University").shortName("VTU").category("EDUCATION").icon("🎓").state("Karnataka").consumerIdLabel("USN (University Seat Number)").consumerIdHint("e.g. 1RV21CS001").build(),
        BillerDto.builder().id("SCHOOL_GENERIC").name("School Fee Payment (Generic)").shortName("School Fee").category("EDUCATION").icon("🎓").state("All India").consumerIdLabel("Admission / Roll Number").consumerIdHint("e.g. 2024-12345").build()
    );

    private static final List<BillerDto> LPG_BILLERS = List.of(
        BillerDto.builder().id("HP_GAS").name("HP Gas – Hindustan Petroleum LPG").shortName("HP Gas").category("LPG").icon("🟢").state("All India").consumerIdLabel("LPG Consumer ID").consumerIdHint("e.g. HP123456789").build(),
        BillerDto.builder().id("BHARAT_GAS").name("Bharat Gas – BPCL LPG").shortName("Bharat Gas").category("LPG").icon("🔵").state("All India").consumerIdLabel("LPG Consumer ID").consumerIdHint("e.g. BG987654321").build(),
        BillerDto.builder().id("INDANE_GAS").name("Indane Gas – IOCL LPG").shortName("Indane Gas").category("LPG").icon("🔴").state("All India").consumerIdLabel("LPG Consumer ID").consumerIdHint("e.g. IG456789012").build()
    );

    private static final List<BillerDto> LANDLINE_BILLERS = List.of(
        BillerDto.builder().id("BSNL_LANDLINE").name("BSNL Landline / PSTN").shortName("BSNL").category("LANDLINE").icon("☎").state("All India").consumerIdLabel("Telephone Number").consumerIdHint("e.g. 01123456789").build(),
        BillerDto.builder().id("MTNL_DELHI").name("MTNL Delhi – Landline").shortName("MTNL Delhi").category("LANDLINE").icon("☎").state("Delhi").consumerIdLabel("Telephone / Account Number").consumerIdHint("e.g. 01123456789").build(),
        BillerDto.builder().id("MTNL_MUMBAI").name("MTNL Mumbai – Landline").shortName("MTNL Mumbai").category("LANDLINE").icon("☎").state("Maharashtra").consumerIdLabel("Telephone / Account Number").consumerIdHint("e.g. 02223456789").build(),
        BillerDto.builder().id("AIRTEL_LANDLINE").name("Airtel Landline / Broadband").shortName("Airtel").category("LANDLINE").icon("☎").state("All India").consumerIdLabel("Airtel Account Number").consumerIdHint("e.g. 123456789012").build(),
        BillerDto.builder().id("ACT_LANDLINE").name("ACT Fibernet – Voice").shortName("ACT").category("LANDLINE").icon("☎").state("Multi").consumerIdLabel("Subscriber ID").consumerIdHint("e.g. ACT12345678").build()
    );

    private static final Map<String, List<BillerDto>> BILLER_CATALOGUE = Map.ofEntries(
        Map.entry("ELECTRICITY",   ELECTRICITY_BILLERS),
        Map.entry("GAS",           GAS_BILLERS),
        Map.entry("WATER",         WATER_BILLERS),
        Map.entry("BROADBAND",     BROADBAND_BILLERS),
        Map.entry("DTH",           DTH_BILLERS),
        Map.entry("FASTAG",        FASTAG_BILLERS),
        Map.entry("MUNICIPAL_TAX", MUNICIPAL_TAX_BILLERS),
        Map.entry("CREDIT_CARD",   CREDIT_CARD_BILLERS),
        Map.entry("EDUCATION",     EDUCATION_BILLERS),
        Map.entry("LPG",           LPG_BILLERS),
        Map.entry("LANDLINE",      LANDLINE_BILLERS)
    );

    // ── Endpoints ─────────────────────────────────────────────────

    @GetMapping("/billers")
    public ResponseEntity<List<BillerDto>> getBillers(@RequestParam(defaultValue = "ELECTRICITY") String category) {
        log.info("[BBPS] getBillers category={}", category);
        List<BillerDto> billers = BILLER_CATALOGUE.getOrDefault(category.toUpperCase(), List.of());
        return ResponseEntity.ok(billers);
    }

    @PostMapping("/bill/fetch")
    public ResponseEntity<FetchedBillDto> fetchBillAndTrack(
            @RequestBody BillFetchRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("[BBPS] fetchBill billerId={} consumerId={}", request.getBillerId(), request.getConsumerId());

        BillerDto biller = BILLER_CATALOGUE.values().stream()
            .flatMap(List::stream)
            .filter(b -> b.getId().equals(request.getBillerId()))
            .findFirst()
            .orElse(null);

        String billerName = biller != null ? biller.getName() : request.getBillerId();

        LocalDate now = LocalDate.now();
        LocalDate periodFrom = now.withDayOfMonth(1).minusMonths(1);
        LocalDate periodTo = now.withDayOfMonth(1).minusDays(1);
        LocalDate dueDate = now.withDayOfMonth(20);
        if (dueDate.isBefore(now)) dueDate = dueDate.plusMonths(1);

        String category = biller != null ? biller.getCategory() : "ELECTRICITY";
        int units = "ELECTRICITY".equals(category) ? 150 + ThreadLocalRandom.current().nextInt(80) : 0;
        BigDecimal amount = switch (category) {
            case "ELECTRICITY"   -> BigDecimal.valueOf(Math.round(units * 6.50 + 180.0)).setScale(2);
            case "GAS"           -> BigDecimal.valueOf(680 + ThreadLocalRandom.current().nextInt(400)).setScale(2);
            case "WATER"         -> BigDecimal.valueOf(320 + ThreadLocalRandom.current().nextInt(200)).setScale(2);
            case "BROADBAND"     -> BigDecimal.valueOf(699 + ThreadLocalRandom.current().nextInt(400)).setScale(2);
            case "DTH"           -> BigDecimal.valueOf(299 + ThreadLocalRandom.current().nextInt(300)).setScale(2);
            case "FASTAG"        -> BigDecimal.valueOf(200 + ThreadLocalRandom.current().nextInt(300)).setScale(2);
            case "MUNICIPAL_TAX" -> BigDecimal.valueOf(4500 + ThreadLocalRandom.current().nextInt(6000)).setScale(2);
            case "CREDIT_CARD"   -> BigDecimal.valueOf(3200 + ThreadLocalRandom.current().nextInt(12000)).setScale(2);
            case "EDUCATION"     -> BigDecimal.valueOf(15000 + ThreadLocalRandom.current().nextInt(25000)).setScale(2);
            case "LPG"           -> BigDecimal.valueOf(850 + ThreadLocalRandom.current().nextInt(200)).setScale(2);
            case "LANDLINE"      -> BigDecimal.valueOf(320 + ThreadLocalRandom.current().nextInt(300)).setScale(2);
            default              -> BigDecimal.valueOf(999).setScale(2);
        };

        String billNumber = "BBPS-" + request.getBillerId().substring(0, Math.min(4, request.getBillerId().length()))
            + "-" + (System.currentTimeMillis() % 1_000_000L);

        // Track the fetch so we can correlate with payment later
        if (principal != null) {
            AppUser user = loggedInUserService.resolve(principal);
            billFetchRepository.save(BbpsBillFetch.builder()
                .communityId(user.getCommunity().getId())
                .userId(user.getId())
                .billerId(request.getBillerId())
                .billerName(billerName)
                .consumerId(request.getConsumerId())
                .billNumber(billNumber)
                .billAmount(amount)
                .dueDate(dueDate)
                .status("FETCHED")
                .build());
        }

        FetchedBillDto bill = FetchedBillDto.builder()
            .billerId(request.getBillerId())
            .billerName(billerName)
            .consumerId(request.getConsumerId())
            .customerName("Resident Account")
            .billNumber(billNumber)
            .billDate(periodFrom.format(DateTimeFormatter.ISO_LOCAL_DATE))
            .dueDate(dueDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
            .amount(amount)
            .units(units > 0 ? units : null)
            .periodFrom(periodFrom.format(DateTimeFormatter.ISO_LOCAL_DATE))
            .periodTo(periodTo.format(DateTimeFormatter.ISO_LOCAL_DATE))
            .status("DUE")
            .build();

        return ResponseEntity.ok(bill);
    }

    @PostMapping("/bill/pay")
    public ResponseEntity<BillPaymentResponse> payBill(
            @RequestBody BillPayRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("[BBPS] payBill billNumber={} billerId={} amount={} method={}",
            request.getBillNumber(), request.getBillerId(), request.getAmount(), request.getMethod());

        BigDecimal amount = request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO;

        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();

        // Create a tracked payment_order — referenced in webhook/callback to mark PAID
        String idempotencyKey = "BBPS-" + request.getBillNumber() + "-" + user.getId();
        PaymentOrder paymentOrder = paymentOrderService.createOrder(
            CreatePaymentOrderRequest.builder()
                .communityId(communityId)
                .idempotencyKey(idempotencyKey)
                .gateway(PaymentGateway.UPI_DIRECT)
                .referenceType(PaymentReferenceType.BBPS_BILL)
                .referenceId(0L)      // BBPS bills have no internal row ID; callback identifies by billNumber
                .payerUserId(user.getId())
                .amount(amount)
                .build()
        );

        // Update the bill fetch record with this payment order
        billFetchRepository.findAll().stream()
            .filter(f -> request.getBillNumber().equals(f.getBillNumber())
                      && f.getPaymentOrderId() == null
                      && communityId.equals(f.getCommunityId()))
            .findFirst()
            .ifPresent(f -> {
                f.setPaymentOrderId(paymentOrder.getId());
                f.setStatus("PAYMENT_INITIATED");
                billFetchRepository.save(f);
            });

        String pa = "bbps@setu";
        String pn = "Mana+Pay+BBPS";
        String tr = paymentOrder.getIdempotencyKey();
        String tn = "Bill+Payment";

        String upiUrl = String.format(
            "upi://pay?pa=%s&pn=%s&am=%s&cu=INR&tr=%s&tn=%s",
            pa, pn, amount.toPlainString(), tr, tn);
        String phonePeUrl = String.format(
            "phonepe://pay?pa=%s&pn=%s&am=%s&cu=INR&tr=%s",
            pa, pn, amount.toPlainString(), tr);

        return ResponseEntity.ok(BillPaymentResponse.builder()
            .upiIntentUrl(upiUrl)
            .phonePeUrl(phonePeUrl)
            .transactionRef(tr)
            .paymentOrderId(paymentOrder.getId())
            .amount(amount)
            .build());
    }

    /**
     * UPI intent callback — called by the mobile app after UPI app returns.
     * Marks the payment_order SUCCESS and the bill fetch record PAID.
     * NOTE: Real BBPS aggregator (Setu/Razorpay BillPay) will call a separate
     * webhook instead. Wire that webhook once credentials are provisioned.
     */
    @PostMapping("/payment/callback")
    public ResponseEntity<Map<String, Object>> paymentCallback(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        String idempotencyKey = body.get("transactionRef");
        String upiTxnId       = body.getOrDefault("upiTransactionId", "UNKNOWN");
        log.info("[BBPS] payment callback transactionRef={} upiTxnId={}", idempotencyKey, upiTxnId);

        try {
            paymentOrderService.markSuccess(idempotencyKey, upiTxnId, null);

            billFetchRepository.findAll().stream()
                .filter(f -> idempotencyKey != null && idempotencyKey.startsWith("BBPS-")
                          && f.getPaymentOrderId() != null
                          && "PAYMENT_INITIATED".equals(f.getStatus()))
                .filter(f -> {
                    // key format: BBPS-<billNumber>-<userId>
                    String suffix = idempotencyKey.substring(5);
                    int lastDash = suffix.lastIndexOf('-');
                    return lastDash > 0 && suffix.substring(0, lastDash).equals(f.getBillNumber());
                })
                .findFirst()
                .ifPresent(f -> {
                    f.setStatus("PAID");
                    f.setPaidAt(LocalDateTime.now());
                    billFetchRepository.save(f);
                });

            return ResponseEntity.ok(Map.of("status", "SUCCESS", "transactionRef", idempotencyKey));
        } catch (Exception e) {
            log.error("[BBPS] callback failed for transactionRef={}: {}", idempotencyKey, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("status", "FAILED", "error", e.getMessage()));
        }
    }
}
