package com.samourai.whirlpool.client.event;

import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolEvent;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoData;

public class WalletStartEvent extends WhirlpoolEvent {
  private WhirlpoolWallet whirlpoolWallet;
  private UtxoData utxoData;

  public WalletStartEvent(WhirlpoolWallet whirlpoolWallet, UtxoData utxoData) {
    super();
    this.whirlpoolWallet = whirlpoolWallet;
    this.utxoData = utxoData;
  }

  public WhirlpoolWallet getWhirlpoolWallet() {
    return whirlpoolWallet;
  }

  public UtxoData getUtxoData() {
    return utxoData;
  }
}
