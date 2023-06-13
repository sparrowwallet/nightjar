package com.samourai.wallet.bip47.rpc.java;

import com.samourai.wallet.bip47.rpc.secretPoint.ISecretPoint;
import com.samourai.wallet.bip47.rpc.secretPoint.ISecretPointFactory;
import com.samourai.wallet.hd.HD_Address;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;

public class SecretPointFactoryJava implements ISecretPointFactory {

    private static SecretPointFactoryJava instance = null;
    public static SecretPointFactoryJava getInstance() {
        if (instance == null) {
            instance = new SecretPointFactoryJava();
        }
        return instance;
    }

    public SecretPointFactoryJava() {}

    @Override
    public ISecretPoint newSecretPoint(byte[] dataPrv, byte[] dataPub) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, InvalidKeyException {
        return new SecretPointJava(dataPrv, dataPub);
    }

    public ISecretPoint newSecretPoint(HD_Address address, byte[] dataPub) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, InvalidKeyException {
        return new SecretPointJava(address.getECKey().getPrivKeyBytes(), dataPub);
    }
}
