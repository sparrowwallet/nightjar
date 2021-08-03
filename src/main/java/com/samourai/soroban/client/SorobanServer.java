package com.samourai.soroban.client;

public enum SorobanServer {
  MAINNET("http://sorob4sg7yiopktgz4eom7hl5mcodr6quvhmdpljl5qqhmt6po7oebid.onion"),
  TESTNET("http://sorob4sg7yiopktgz4eom7hl5mcodr6quvhmdpljl5qqhmt6po7oebid.onion");

  private String url;

  SorobanServer(String url) {
    this.url = url;
  }

  public String getUrl() {
    return url;
  }

  public static SorobanServer get(boolean isTestnet) {
    return isTestnet ? SorobanServer.TESTNET : SorobanServer.MAINNET;
  }
}
