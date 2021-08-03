package com.samourai.wallet.crypto.impl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

public class ECDHKeySet {

    public byte[] masterKey;
    public byte[] encryptionKey;
    public byte[] hmacKey;
    public byte[] ivClient;
    public byte[] ivServer;

    public long counterIn;
    public long counterOut;

    public ECDHKeySet (byte[] masterKey, byte[] serverPubkey, byte[] clientPubkey, String provider) throws NoSuchProviderException, NoSuchAlgorithmException {
        this.masterKey = masterKey;
        MessageDigest hash = MessageDigest.getInstance("RIPEMD128", provider);
        byte[] t = new byte[masterKey.length + 1];
        System.arraycopy(masterKey, 0, t, 0, masterKey.length);
        t[t.length - 1] = 0x00;

        hash.update(t);

        encryptionKey = hash.digest();

        t[t.length - 1] = 0x01;
        hash.update(t);

        hmacKey = hash.digest();

        byte[] a1 = new byte[masterKey.length + serverPubkey.length];
        byte[] a2 = new byte[masterKey.length + serverPubkey.length];

        System.arraycopy(masterKey, 0, a1, 0, masterKey.length);
        System.arraycopy(serverPubkey, 0, a1, masterKey.length, serverPubkey.length);

        System.arraycopy(masterKey, 0, a2, 0, masterKey.length);
        System.arraycopy(clientPubkey, 0, a2, masterKey.length, clientPubkey.length);

        ivClient = new byte[8];
        ivServer = new byte[8];

        hash.update(a1);
        byte[] b1 = hash.digest();
        System.arraycopy(b1, 0, ivServer, 0, 8);

        hash.update(a2);
        byte[] b2 = hash.digest();
        System.arraycopy(b2, 0, ivClient, 0, 8);

//		return hash.digest();
    }

}