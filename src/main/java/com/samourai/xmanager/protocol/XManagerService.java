package com.samourai.xmanager.protocol;

public enum XManagerService {
  WHIRLPOOL(
      "bc1qxya59zn6fgenfls0pedt0xqkagd33fcfc5s04n", "tb1q6m3urxjc8j2l8fltqj93jarmzn0975nnxuymnx"),
  SAAS("bc1qsnefhrmd8gs6a4j7fzxfy4ay52j78agl6rsw36", "tb1q6y29zmgw2ajgmqrnpqrf0ejtyrqsh3ehd6wjlk"),
  RICOCHET(
      "bc1qptv26ag03um6dhvcrfjh29wlez64mdufw77z8g", "tb1q6y29zmgw2ajgmqrnpqrf0ejtyrqsh3ehd6wjlk"),
  BIP47("bc1quq8h89h6j9f6nv5fyqjjav2auzah799lq2j9fa", "tb1q6y29zmgw2ajgmqrnpqrf0ejtyrqsh3ehd6wjlk"),
  OXT_RESEARCH(
      "bc1qj2sac3nvaettzz5egmtnk223vjrey2hxyx9j9k", "tb1q6y29zmgw2ajgmqrnpqrf0ejtyrqsh3ehd6wjlk");

  private String defaultAddressMainnet;
  private String defaultAddressTestnet;

  private XManagerService(String defaultAddressMainnet, String defaultAddressTestnet) {
    this.defaultAddressMainnet = defaultAddressMainnet;
    this.defaultAddressTestnet = defaultAddressTestnet;
  }

  public String getDefaultAddressMainnet() {
    return defaultAddressMainnet;
  }

  public String getDefaultAddressTestnet() {
    return defaultAddressTestnet;
  }

  public String getDefaultAddress(boolean testnet) {
    return testnet ? defaultAddressTestnet : defaultAddressMainnet;
  }
}
