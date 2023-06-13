package com.samourai.wallet.send.provider;

import com.samourai.wallet.hd.HD_Address;
import org.bitcoinj.core.ECKey;

public interface UtxoKeyProvider {

    ECKey _getPrivKey(String utxoHash, int utxoIndex) throws Exception;
    HD_Address getAddress(String utxoHash, int utxoIndex) throws Exception;
}
