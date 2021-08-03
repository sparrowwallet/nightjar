package com.samourai.wallet.crypto.impl;

import org.bouncycastle.util.Arrays;

public class EncryptedMessage {
    private static final int HMAC_LENGTH = 20; // static length expected

    public byte[] hmac;
    public byte[] payload;

    public EncryptedMessage (byte[] hmac, byte[] payload) {
        this.hmac = hmac;
        this.payload = payload;
    }

    public byte[] serialize() throws Exception {
        if (hmac.length != HMAC_LENGTH) {
            throw new Exception("Invalid HMAC length");
        }
        return Arrays.concatenate(hmac, payload);
    }

    public static EncryptedMessage unserialize(byte[] serialized) throws Exception {
        byte[] hmac = Arrays.copyOfRange(serialized, 0, HMAC_LENGTH);
        byte[] payload = Arrays.copyOfRange(serialized, HMAC_LENGTH, serialized.length);
        return new EncryptedMessage(hmac, payload);
    }
}