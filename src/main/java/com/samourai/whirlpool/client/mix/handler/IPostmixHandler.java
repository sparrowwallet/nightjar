package com.samourai.whirlpool.client.mix.handler;

import org.bitcoinj.core.NetworkParameters;

public interface IPostmixHandler {

  String computeReceiveAddress(NetworkParameters params) throws Exception;

  void onRegisterOutput();

  void onMixFail();
}
