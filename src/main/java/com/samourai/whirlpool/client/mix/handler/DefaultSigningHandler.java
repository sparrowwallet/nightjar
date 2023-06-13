package com.samourai.whirlpool.client.mix.handler;

import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.send.SendFactoryGeneric;
import com.samourai.wallet.send.exceptions.SignTxException;
import com.samourai.wallet.send.provider.UtxoKeyProvider;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;

public class DefaultSigningHandler implements SigningHandler {
    @Override
    public String signMessage(HD_Address hdAddress, String message) {
        return hdAddress.getECKey().signMessage(message);
    }

    @Override
    public Transaction signTx0Transaction(Transaction transaction, UtxoKeyProvider utxoKeyProvider) throws SignTxException {
        byte[] signedTx = signTx0Transaction(transaction.bitcoinSerialize(), utxoKeyProvider);
        if(signedTx != null) {
            return new Transaction(transaction.getParams(), signedTx);
        }

        SendFactoryGeneric.getInstance().signTransaction(transaction, getUtxoKeyProvider(utxoKeyProvider));
        return transaction;
    }

    protected byte[] signTx0Transaction(byte[] transactionBytes, UtxoKeyProvider utxoKeyProvider) throws SignTxException {
        return null;
    }

    protected UtxoKeyProvider getUtxoKeyProvider(UtxoKeyProvider utxoKeyProvider) {
        return utxoKeyProvider;
    }

    @Override
    public byte[] signMixTransaction(byte[] transaction, int inputIndex, HD_Address hdAddress, long spendAmount) {
        final SegwitAddress segwitAddress = new SegwitAddress(hdAddress.getECKey(), hdAddress.getParams());
        final Script redeemScript = segwitAddress.segWitRedeemScript();
        final Script scriptCode = redeemScript.scriptCode();

        Transaction tx = new Transaction(hdAddress.getParams(), transaction);
        TransactionSignature sig = tx.calculateWitnessSignature(inputIndex, hdAddress.getECKey(), scriptCode, Coin.valueOf(spendAmount), Transaction.SigHash.ALL, false);
        return sig.encodeToBitcoin();
    }
}
