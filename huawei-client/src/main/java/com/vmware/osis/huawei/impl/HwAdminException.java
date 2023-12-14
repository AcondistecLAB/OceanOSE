package com.vmware.osis.huawei.impl;

public class HwAdminException extends RuntimeException {

  private final int statusCode;

  public HwAdminException(int statusCode) {
    this.statusCode = statusCode;
  }

  public HwAdminException(int statusCode, String messageCode) {
    super(messageCode);
    this.statusCode = statusCode;
  }

  public HwAdminException(int statusCode, String messageCode, Throwable cause) {
    super(messageCode, cause);
    this.statusCode = statusCode;
  }

  public int status() {
    return statusCode;
  }
}