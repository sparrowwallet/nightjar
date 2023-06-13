package com.samourai.whirlpool.client.mix.handler;

import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.send.exceptions.SignTxException;
import com.samourai.wallet.send.provider.UtxoKeyProvider;
import org.bitcoinj.core.Transaction;

public interface SigningHandler {
    String signMessage(HD_Address hdAddress, String message);
    Transaction signTx0Transaction(Transaction transaction, UtxoKeyProvider utxoKeyProvider) throws SignTxException;
    byte[] signMixTransaction(byte[] transaction, int inputIndex, HD_Address hdAddress, long spendAmount);
}
