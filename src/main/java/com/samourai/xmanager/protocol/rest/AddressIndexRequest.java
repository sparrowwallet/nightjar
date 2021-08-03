package com.samourai.xmanager.protocol.rest;

import org.hibernate.validator.constraints.NotEmpty;

public class AddressIndexRequest {
  @NotEmpty public String id;

  public AddressIndexRequest() {}

  public AddressIndexRequest(String id) {
    this.id = id;
  }
}
