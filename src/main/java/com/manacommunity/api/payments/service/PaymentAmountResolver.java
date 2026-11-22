package com.manacommunity.api.payments.service;

import com.manacommunity.api.commerce.core.model.CommerceOrder;
import com.manacommunity.api.commerce.core.model.CommerceOrderStatus;
import com.manacommunity.api.commerce.core.repository.CommerceOrderRepository;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.groupbuying.model.GroupBuyOrder;
import com.manacommunity.api.groupbuying.model.OrderStatus;
import com.manacommunity.api.groupbuying.repository.GroupBuyOrderRepository;
import com.manacommunity.api.payments.repository.CashfreeOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Set;

/**
 * Single server-side authority for "how much is this payment actually for?".
 *
 * <p>Payment-order creation endpoints receive {@code (referenceType, referenceId)} —
 * i.e. <em>what</em> is being paid for — plus a client-supplied {@code amount}.
 * The client-supplied amount is advisory only: a patched mobile bundle can send
 * {@code amount: 1} for a ₹12,000 order, and whatever amount the gateway order is
 * created with is what actually gets collected. This resolver loads the referenced
 * record, checks it belongs to the caller, checks it has not already been paid, and
 * returns the <strong>stored</strong> amount.
 *
 * <p>Resolution rules:
 * <ul>
 *   <li>An unknown / unsupported {@code referenceType} is rejected (400).</li>
 *   <li>A missing referenced record is rejected (404).</li>
 *   <li>A record belonging to another user or another community is rejected (403).</li>
 *   <li>A record already paid (or refunded / cancelled) is rejected (409).</li>
 *   <li>A record with no usable stored amount is rejected (409).</li>
 * </ul>
 * There is deliberately <strong>no</strong> fall-back to the client amount on any of
 * those paths — a failed resolution must never become a client-priced order.
 *
 * @see #CLIENT_PRICED_REFERENCE_TYPES for the one documented exception
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentAmountResolver {

    /**
     * Reference types that are still priced by the client, because the schema holds
     * no server-side amount for them anywhere.
     *
     * <p>{@code SPORTS_REGISTRATION} is the only member. Its {@code referenceId} is a
     * {@code SportsEvent} id (see {@code CashfreePaymentService#confirmSportsRegistration}),
     * and neither {@code SportsEvent} nor {@code SportsEventRegistration} has a fee /
     * price / entry-cost column — the fee exists only in the organiser's head and in
     * whatever the app screen shows. {@code SportsTournamentPlayerPayment.amount} is an
     * admin-entered record of cash already collected, keyed by (tournament, playerKey),
     * so it cannot price a registration either.
     *
     * <p>Rejecting the type would break the one Cashfree flow that is definitely live
     * (it is the only type with a business-confirmation handler), so it keeps the old
     * behaviour — but loudly, and alone. The real fix is a server-side registration-fee
     * column on the sports event, after which this set should become empty and the
     * {@code case} below should resolve it like any other type.
     */
    private static final Set<String> CLIENT_PRICED_REFERENCE_TYPES = Set.of("SPORTS_REGISTRATION");

    /** Gateway order statuses that mean money has already been collected for a reference. */
    private static final String STATUS_PAID = "PAID";

    private final GroupBuyOrderRepository groupBuyOrderRepository;
    private final CommerceOrderRepository commerceOrderRepository;
    private final CashfreeOrderRepository cashfreeOrderRepository;

    /**
     * The outcome of a resolution.
     *
     * @param referenceType normalised (trimmed, upper-cased) reference type
     * @param amount        the amount the gateway order must be created with
     * @param serverDerived {@code true} when {@code amount} came from a stored record;
     *                      {@code false} when it is a client-priced exception
     */
    public record ResolvedAmount(String referenceType, BigDecimal amount, boolean serverDerived) {
    }

    /**
     * Resolves the payable amount for {@code (referenceType, referenceId)}.
     *
     * @param referenceType     client-supplied business discriminator
     * @param referenceId       PK of the referenced business record
     * @param callerCommunityId community of the authenticated caller
     * @param callerUserId      id of the authenticated caller
     * @param clientAmount      the client-supplied amount — used only for mismatch
     *                          logging, and as the amount itself for the documented
     *                          {@link #CLIENT_PRICED_REFERENCE_TYPES} exception
     * @throws IllegalArgumentException for an unsupported reference type / bad input (400)
     * @throws ResourceNotFoundException when the referenced record does not exist (404)
     * @throws AccessDeniedException when the record belongs to someone else (403)
     * @throws IllegalStateException when the record is already paid or unpayable (409)
     */
    @Transactional(readOnly = true)
    public ResolvedAmount resolve(String referenceType,
                                  Long referenceId,
                                  Long callerCommunityId,
                                  Long callerUserId,
                                  BigDecimal clientAmount) {

        if (referenceType == null || referenceType.isBlank()) {
            throw new IllegalArgumentException("referenceType is required to price a payment.");
        }
        if (referenceId == null) {
            throw new IllegalArgumentException("referenceId is required to price a payment.");
        }
        if (callerUserId == null || callerCommunityId == null) {
            throw new AccessDeniedException("Payment caller could not be identified.");
        }

        String type = referenceType.trim().toUpperCase(Locale.ROOT);

        // A second gateway order for something this caller already paid for is never legitimate.
        rejectIfAlreadyCollected(referenceType, type, referenceId, callerUserId);

        BigDecimal serverAmount = switch (type) {
            case "GROUP_BUY_ORDER" -> groupBuyOrderAmount(referenceId, callerCommunityId, callerUserId);
            case "COMMERCE_ORDER"  -> commerceOrderAmount(referenceId, callerCommunityId, callerUserId);
            default -> null;
        };

        if (serverAmount != null) {
            BigDecimal amount = normalise(serverAmount);
            logMismatch(type, referenceId, callerUserId, clientAmount, amount);
            return new ResolvedAmount(type, amount, true);
        }

        if (CLIENT_PRICED_REFERENCE_TYPES.contains(type)) {
            // Deliberate, documented exception — see CLIENT_PRICED_REFERENCE_TYPES.
            // The amount here is whatever the client said it is; it is NOT verified.
            if (clientAmount == null || clientAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "A positive amount is required for referenceType=" + type + ".");
            }
            log.warn("CLIENT-PRICED PAYMENT: referenceType={} referenceId={} userId={} communityId={} "
                            + "amount=₹{} accepted from the client because no server-side amount exists for "
                            + "this type. Add a server-side fee column and resolve it here.",
                    type, referenceId, callerUserId, callerCommunityId, clientAmount);
            return new ResolvedAmount(type, normalise(clientAmount), false);
        }

        log.warn("Rejected payment order for unsupported referenceType={} referenceId={} userId={} "
                        + "communityId={} clientAmount=₹{}",
                type, referenceId, callerUserId, callerCommunityId, clientAmount);
        throw new IllegalArgumentException(
                "Payments for referenceType '" + referenceType + "' are not supported. "
                        + "Supported types: GROUP_BUY_ORDER, COMMERCE_ORDER, SPORTS_REGISTRATION.");
    }

    // ─── Per-type resolution ──────────────────────────────────────────────────

    /**
     * Group-buy order: the authoritative figure is {@code total_amount}, computed
     * server-side from the deal's current tier price × quantity at checkout.
     *
     * <p>Note on "unpaid": {@code GroupBuyOrder.paymentStatus} is initialised to
     * {@code "PAID"} by {@code GroupBuyingService#joinDeal} / {@code #checkoutGroupBuy}
     * <em>before</em> any gateway call, so it cannot be used as the "not yet paid" test
     * without rejecting every legitimate group-buy payment. The real duplicate-payment
     * guard is {@link #rejectIfAlreadyCollected} (a PAID gateway order for the same
     * reference by the same payer); here we only reject orders in a terminal,
     * unpayable state.
     */
    private BigDecimal groupBuyOrderAmount(Long referenceId, Long callerCommunityId, Long callerUserId) {
        GroupBuyOrder order = groupBuyOrderRepository.findById(referenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Group buy order", referenceId));

        requireOwnership("GROUP_BUY_ORDER", referenceId,
                order.getUser() != null ? order.getUser().getId() : null,
                order.getCommunity() != null ? order.getCommunity().getId() : null,
                callerUserId, callerCommunityId);

        if (order.getStatus() == OrderStatus.CANCELLED
                || order.getStatus() == OrderStatus.REFUNDED
                || "REFUNDED".equalsIgnoreCase(order.getPaymentStatus())) {
            throw new IllegalStateException(
                    "Group buy order " + order.getOrderNumber() + " is " + order.getStatus()
                            + " and cannot be paid for.");
        }

        return requirePayable("GROUP_BUY_ORDER", referenceId, order.getTotalAmount());
    }

    /**
     * Commerce order: {@code total_amount} is computed server-side in
     * {@code CommerceCoreServiceImpl#checkout} from the catalogue price
     * ({@code discountPrice ?? basePrice}) plus server-calculated discount and
     * delivery fee, so it is authoritative.
     */
    private BigDecimal commerceOrderAmount(Long referenceId, Long callerCommunityId, Long callerUserId) {
        CommerceOrder order = commerceOrderRepository.findById(referenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Commerce order", referenceId));

        requireOwnership("COMMERCE_ORDER", referenceId,
                order.getBuyer() != null ? order.getBuyer().getId() : null,
                order.getCommunity() != null ? order.getCommunity().getId() : null,
                callerUserId, callerCommunityId);

        if (STATUS_PAID.equalsIgnoreCase(order.getPaymentStatus())
                || order.getStatus() == CommerceOrderStatus.PAYMENT_AUTHORIZED
                || order.getStatus() == CommerceOrderStatus.CANCELLED
                || order.getStatus() == CommerceOrderStatus.REFUNDED) {
            throw new IllegalStateException(
                    "Commerce order " + order.getOrderNumber() + " is not payable (status="
                            + order.getStatus() + ", paymentStatus=" + order.getPaymentStatus() + ").");
        }

        return requirePayable("COMMERCE_ORDER", referenceId, order.getTotalAmount());
    }

    // ─── Shared guards ────────────────────────────────────────────────────────

    /**
     * Rejects a new gateway order when this caller already has a PAID one for the same
     * business reference. This is the cross-module "already paid" check: it does not
     * depend on each module's own payment flag, which several modules set
     * optimistically at record-creation time.
     *
     * <p>Scoped to the caller because a referenceId is not always unique per payer —
     * SPORTS_REGISTRATION carries a sports event id shared by every registrant, so a
     * reference-wide check would reject everyone after the first payer.
     */
    private void rejectIfAlreadyCollected(String rawReferenceType, String normalisedType,
                                          Long referenceId, Long callerUserId) {
        if (isAlreadyCollected(rawReferenceType, referenceId, callerUserId)
                || (!normalisedType.equals(rawReferenceType)
                        && isAlreadyCollected(normalisedType, referenceId, callerUserId))) {
            log.warn("Rejected duplicate payment order: userId={} already has a PAID Cashfree order for {}:{}",
                    callerUserId, normalisedType, referenceId);
            throw new IllegalStateException(
                    "This item has already been paid for. Refresh and check your payment history.");
        }
    }

    private boolean isAlreadyCollected(String referenceType, Long referenceId, Long callerUserId) {
        return cashfreeOrderRepository.existsByReferenceTypeAndReferenceIdAndUserIdAndStatus(
                referenceType, referenceId, callerUserId, STATUS_PAID);
    }

    private void requireOwnership(String type, Long referenceId,
                                  Long ownerUserId, Long ownerCommunityId,
                                  Long callerUserId, Long callerCommunityId) {
        boolean sameUser      = ownerUserId != null && ownerUserId.equals(callerUserId);
        boolean sameCommunity = ownerCommunityId != null && ownerCommunityId.equals(callerCommunityId);
        if (!sameUser || !sameCommunity) {
            log.warn("Rejected cross-owner payment attempt: {}:{} belongs to userId={} communityId={} "
                            + "but caller is userId={} communityId={}",
                    type, referenceId, ownerUserId, ownerCommunityId, callerUserId, callerCommunityId);
            throw new AccessDeniedException("This item does not belong to you.");
        }
    }

    private BigDecimal requirePayable(String type, Long referenceId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException(
                    "No valid server-side amount is configured for " + type + ":" + referenceId + ".");
        }
        return amount;
    }

    private void logMismatch(String type, Long referenceId, Long callerUserId,
                             BigDecimal clientAmount, BigDecimal serverAmount) {
        if (clientAmount == null) {
            return;
        }
        if (normalise(clientAmount).compareTo(serverAmount) != 0) {
            // A persistent mismatch is either a client bug or someone probing the endpoint.
            log.warn("AMOUNT MISMATCH on payment order: {}:{} userId={} clientAmount=₹{} "
                            + "serverAmount=₹{} — charging the server amount.",
                    type, referenceId, callerUserId, clientAmount, serverAmount);
        }
    }

    private BigDecimal normalise(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
