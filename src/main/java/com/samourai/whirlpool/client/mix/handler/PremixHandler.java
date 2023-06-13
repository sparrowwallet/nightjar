package com.samourai.whirlpool.client.mix.handler;

import com.samourai.wallet.hd.HD_Address;
import com.samourai.whirlpool.client.utils.ClientUtils;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.TransactionSignature;

public class PremixHandler implements IPremixHandler {
  private UtxoWithBalance utxo;
  private HD_Address utxoAddress;
  private String userPreHash;
  private SigningHandler signingHandler;

  public PremixHandler(UtxoWithBalance utxo, HD_Address utxoAddress, String userPreHash, SigningHandler signingHandler) {
    this.utxo = utxo;
    this.utxoAddress = utxoAddress;
    this.userPreHash = userPreHash;
    this.signingHandler = signingHandler;
  }

  @Override
  public UtxoWithBalance getUtxo() {
    return utxo;
  }

  @Override
  public void signTransaction(Transaction tx, int inputIndex, NetworkParameters params)
      throws Exception {
    // TODO SendFactoryGeneric.getInstance().signInput(utxoKey, params, tx, inputIndex);
    long spendAmount = utxo.getBalance();
    signInputSegwit(tx, inputIndex, utxoAddress.getECKey(), spendAmount, params);
  }

  // TODO
  protected void signInputSegwit(
          Transaction tx, int inputIdx, ECKey ecKey, long spendAmount, NetworkParameters params) {
    byte[] sigBytes = signingHandler.signMixTransaction(tx.bitcoinSerialize(), inputIdx, utxoAddress, spendAmount);
    TransactionSignature sig = TransactionSignature.decodeFromBitcoin(sigBytes, false, false);
    final TransactionWitness witness = new TransactionWitness(2);
    witness.setPush(0, sig.encodeToBitcoin());
    witness.setPush(1, ecKey.getPubKey());
    tx.setWitness(inputIdx, witness);
  }

  @Override
  public String signMessage(String message) {
    return signingHandler.signMessage(utxoAddress, message);
  }

  @Override
  public String computeUserHash(String salt) {
    return ClientUtils.sha256Hash(salt + userPreHash);
  }
}
