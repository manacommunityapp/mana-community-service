package com.manacommunity.api.exception;

/**
 * Thrown when the wallet service or ledger database is temporarily unreachable or locked.
 * Guarantees that wallet balance checks and debits fail explicitly without displaying synthetic balances.
 */
public class WalletUnavailableException extends ServiceDegradedException {

    public WalletUnavailableException(String message) {
        super(message, "WALLET", "WALLET_SERVICE_UNAVAILABLE");
    }

    public WalletUnavailableException(String message, Throwable cause) {
        super(message, "WALLET", "WALLET_SERVICE_UNAVAILABLE", cause);
    }
}
