package com.samourai.xmanager.protocol.rest;

import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.Range;

public class VerifyAddressIndexRequest {
  @NotEmpty public String id;
  @NotEmpty public String address;

  @Range(min = 0)
  public int index;

  public VerifyAddressIndexRequest() {
    super();
  }

  public VerifyAddressIndexRequest(String id, String address, int index) {
    this.id = id;
    this.address = address;
    this.index = index;
  }
}
