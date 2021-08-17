package com.samourai.whirlpool.client.event;

import com.samourai.whirlpool.client.wallet.beans.MixProgress;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolEvent;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;

public class MixProgressEvent extends WhirlpoolEvent {
    private WhirlpoolUtxo whirlpoolUtxo;
    private MixProgress mixProgress;

    public MixProgressEvent(WhirlpoolUtxo whirlpoolUtxo, MixProgress mixProgress) {
        this.whirlpoolUtxo = whirlpoolUtxo;
        this.mixProgress = mixProgress;
    }

    public WhirlpoolUtxo getWhirlpoolUtxo() {
        return whirlpoolUtxo;
    }

    public MixProgress getMixProgress() {
        return mixProgress;
    }
}
