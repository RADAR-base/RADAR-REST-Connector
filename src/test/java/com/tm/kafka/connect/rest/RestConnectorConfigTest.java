package com.tm.kafka.connect.rest;

import org.junit.Test;
import org.radarbase.connect.rest.RestSinkConnectorConfig;
import org.radarbase.connect.rest.RestSourceConnectorConfig;

public class RestConnectorConfigTest {
  @Test
  public void docSource() {
    System.out.println(RestSourceConnectorConfig.conf().toRst());
  }

  @Test
  public void docSink() {
    System.out.println(RestSinkConnectorConfig.conf().toRst());
  }
}
