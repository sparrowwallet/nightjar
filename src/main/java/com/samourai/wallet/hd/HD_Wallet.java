package com.samourai.wallet.hd;

import com.google.common.base.Joiner;
import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.util.FormatsUtilGeneric;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolServer;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HD_Wallet {
    private byte[] mSeed = null;
    private String strPassphrase = null;
    private List<String> mWordList = null;

    protected DeterministicKey mRoot = null; // null when created from xpub

    protected ArrayList<HD_Account> mAccounts = null;

    protected NetworkParameters mParams = null;

    private HD_Wallet() { ; }

    /*
    create from seed+passphrase
     */
    public HD_Wallet(int purpose, MnemonicCode mc, NetworkParameters mParams, byte[] mSeed, String strPassphrase, int nbAccounts) throws MnemonicException.MnemonicLengthException {
        this(purpose, mc.toMnemonic(mSeed), mParams, mSeed, strPassphrase, nbAccounts);
    }

    public HD_Wallet(int purpose, List<String> wordList, WhirlpoolServer whirlpoolServer, byte[] mSeed, String strPassphrase, int nbAccounts) {
        this(purpose, wordList, whirlpoolServer.getParams(), mSeed, strPassphrase, nbAccounts);
    }

    protected HD_Wallet(int purpose, List<String> wordList, NetworkParameters mParams, byte[] mSeed, String strPassphrase, int nbAccounts) {
        this(mSeed, strPassphrase, wordList, mParams);

        // compute rootKey for accounts
        this.mRoot = computeRootKey(purpose, mWordList, strPassphrase, mParams);

        // create accounts
        mAccounts = new ArrayList<HD_Account>();
        for(int i = 0; i < nbAccounts; i++) {
            String acctName = String.format("account %02d", i);
            mAccounts.add(new HD_Account(mParams, mRoot, acctName, i));
        }
    }

    protected HD_Wallet(int purpose, HD_Wallet inputWallet, int nbAccounts) {
        this(purpose, inputWallet.mWordList, inputWallet.mParams, inputWallet.mSeed, inputWallet.strPassphrase, nbAccounts);
    }
    public HD_Wallet(int purpose, HD_Wallet inputWallet) {
        this(purpose, inputWallet, 1);
    }

    /*
    create from account xpub key(s)
     */
    public HD_Wallet(NetworkParameters params, String[] xpub) throws AddressFormatException {

        mParams = params;
        mAccounts = new ArrayList<HD_Account>();
        for(int i = 0; i < xpub.length; i++) {
            mAccounts.add(new HD_Account(mParams, xpub[i], "", i));
        }

    }

    protected HD_Wallet(byte[] mSeed, String strPassphrase, List<String> mWordList, NetworkParameters mParams) {
        this.mSeed = mSeed;
        this.strPassphrase = strPassphrase;
        this.mWordList = mWordList;
        this.mParams = mParams;
    }

    private static DeterministicKey computeRootKey(int purpose, List<String> mWordList, String strPassphrase, NetworkParameters params) {
        byte[] hd_seed = MnemonicCode.toSeed(mWordList, strPassphrase);
        DeterministicKey mKey = HDKeyDerivation.createMasterPrivateKey(hd_seed);
        DeterministicKey t1 = HDKeyDerivation.deriveChildKey(mKey, purpose|ChildNumber.HARDENED_BIT);
        int coin = FormatsUtilGeneric.getInstance().isTestNet(params) ? (1 | ChildNumber.HARDENED_BIT) : ChildNumber.HARDENED_BIT;
        DeterministicKey rootKey = HDKeyDerivation.deriveChildKey(t1, coin);
        return rootKey;
    }

    public String getSeedHex() {
        return org.bouncycastle.util.encoders.Hex.toHexString(mSeed);
    }

    public String getMnemonic() {
        return Joiner.on(" ").join(mWordList);
    }

    public String getPassphrase() {
        return strPassphrase;
    }

    public List<HD_Account> getAccounts() {
        return mAccounts;
    }

    public NetworkParameters getParams() {
        return mParams;
    }

    public HD_Account getAccount(int accountId) {
        return mAccounts.get(accountId);
    }

    public HD_Account getAccountAt(int accountIdx) {
        return new HD_Account(mParams, mRoot, "", accountIdx);
    }

    public void addAccount() {
        String strName = String.format("Account %d", mAccounts.size());
        mAccounts.add(new HD_Account(mParams, mRoot, strName, mAccounts.size()));
    }

    public String[] getXPUBs() {

        String[] ret = new String[mAccounts.size()];

        for(int i = 0; i < mAccounts.size(); i++) {
            ret[i] = mAccounts.get(i).xpubstr();
        }

        return ret;
    }

    public byte[] getFingerprint() {

        List<String> wordList = Arrays.asList(getMnemonic().split("\\s+"));
        String passphrase = getPassphrase();

        byte[] hd_seed = MnemonicCode.toSeed(wordList, passphrase.toString());
        DeterministicKey mKey = HDKeyDerivation.createMasterPrivateKey(hd_seed);
        int fp = mKey.getFingerprint();

        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(fp);
        byte[] buf = bb.array();

        return buf;

    }

    public HD_Address getAddressAt(int account, int chain, int idx) {
        return getAccountAt(account).getChain(chain).getAddressAt(idx);
    }

    public SegwitAddress getSegwitAddressAt(int account, int chain, int idx) {
        HD_Address addr = getAddressAt(account, chain, idx);
        SegwitAddress segwitAddress = new SegwitAddress(addr.getPubKey(), mParams);
        return segwitAddress;
    }

    public HD_Address getAddressAt(int account, UnspentOutput utxo) {
        return getAddressAt(account, utxo.computePathChainIndex(), utxo.computePathAddressIndex());
    }
}
