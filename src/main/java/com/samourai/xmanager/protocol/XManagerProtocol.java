package com.samourai.xmanager.protocol;

public class XManagerProtocol {
  /** Current protocol version. */
  public static final String PROTOCOL_VERSION = "1.0";

  /** Header specifying the protocol version. */
  public static final String HEADER_PROTOCOL_VERSION = "xmanagerVersion";

  private static XManagerProtocol instance;

  private XManagerProtocol() {}

  public static XManagerProtocol getInstance() {
    if (instance == null) {
      instance = new XManagerProtocol();
    }
    return instance;
  }

  public String getUrlAddress(String server) {
    return server + XManagerEndpoint.REST_ADDRESS;
  }

  public String getUrlAddressIndex(String server) {
    return server + XManagerEndpoint.REST_ADDRESS_INDEX;
  }

  public String getUrlVerifyAddressIndex(String server) {
    return server + XManagerEndpoint.REST_VERIFY_ADDRESS_INDEX;
  }
}
