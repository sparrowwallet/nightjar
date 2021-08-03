package com.samourai.wallet.segwit;

import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_Wallet;
import org.bitcoinj.core.NetworkParameters;

public class BIP84Wallet {
    private HD_Wallet wallet;
    private NetworkParameters params;

    public BIP84Wallet(HD_Wallet bip84w, NetworkParameters params) {
        this.wallet = bip84w;
        this.params = params;
    }

    public HD_Wallet getWallet() {
        return wallet;
    }

    public SegwitAddress getAddressAt(int chain, int idx) {
        HD_Address addr = getWallet().getAccount(0).getChain(chain).getAddressAt(idx);
        SegwitAddress segwitAddress = new SegwitAddress(addr.getPubKey(), params);
        return segwitAddress;
    }

    public SegwitAddress getAddressAt(int account, int chain, int idx) {
        HD_Address addr = getWallet().getAccountAt(account).getChain(chain).getAddressAt(idx);
        SegwitAddress segwitAddress = new SegwitAddress(addr.getPubKey(), params);
        return segwitAddress;
    }

    public NetworkParameters getParams() {
        return params;
    }
}
