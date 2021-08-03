package com.samourai.xmanager.protocol;

public enum XManagerEnv {
  MAINNET(
      "https://xm.samourai.io:8080",
      "http://n3hf4erg6ils232ufhezoynjbk57fjgasj5gudthgqc2rt5jfmnp6mid.onion"),
  TESTNET(
      "https://xm.samourai.io:8081",
      "http://ns7puljkieiuqqtoswmy5t5rm4gtqh67vzyq6ec446tbzkerct6gzjyd.onion");

  private String urlClear;
  private String urlOnion;

  XManagerEnv(String urlClear, String urlOnion) {
    this.urlClear = urlClear;
    this.urlOnion = urlOnion;
  }

  public String getUrl(boolean onion) {
    return onion ? urlOnion : urlClear;
  }

  public String getUrlClear() {
    return urlClear;
  }

  public String getUrlOnion() {
    return urlOnion;
  }

  public static XManagerEnv get(boolean isTestnet) {
    return isTestnet ? XManagerEnv.TESTNET : XManagerEnv.MAINNET;
  }
}
