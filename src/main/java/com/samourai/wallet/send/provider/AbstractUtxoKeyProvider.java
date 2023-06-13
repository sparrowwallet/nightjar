package com.samourai.wallet.send.provider;

import org.bitcoinj.core.ECKey;

public abstract class AbstractUtxoKeyProvider implements UtxoKeyProvider {
    @Override
    public ECKey _getPrivKey(String utxoHash, int utxoIndex) throws Exception {
        return getAddress(utxoHash, utxoIndex).getECKey();
    }
}
