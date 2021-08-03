package com.samourai.xmanager.protocol.rest;

public class AddressIndexResponse {
  public String address;
  public int index;

  public AddressIndexResponse() {
    super();
  }

  public AddressIndexResponse(String address, int index) {
    this.address = address;
    this.index = index;
  }
}
