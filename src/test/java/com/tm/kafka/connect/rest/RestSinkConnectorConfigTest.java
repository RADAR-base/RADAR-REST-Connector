package com.tm.kafka.connect.rest;

import org.junit.Test;
import org.radarbase.connect.rest.RestSinkConnectorConfig;

public class RestSinkConnectorConfigTest {
  @Test
  public void doc() {
    System.out.println(RestSinkConnectorConfig.conf().toRst());
  }
}
