package com.sirjanhansda.pods.wallets.controller;

import jakarta.persistence.Enumerated;

// The wallet PUT Request is in this format
public class WalletPUTRequest{

    public enum Action {
        debit,
        credit
    }

    @Enumerated
    public Action action;
    public Integer amount;

}
