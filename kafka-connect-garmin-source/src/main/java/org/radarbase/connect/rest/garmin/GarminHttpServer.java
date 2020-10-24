package org.radarbase.connect.rest.garmin;

import java.io.IOException;
import java.net.URI;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.radarbase.connect.rest.garmin.generator.DataGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GarminHttpServer {

  private static final Logger log = LoggerFactory.getLogger(GarminHttpServer.class);
  private HttpServer httpServer;
  private DataGenerator dataGenerator;
  private GarminRestSourceConnectorConfig restSourceConnectorConfig;

  public void initialize(GarminRestSourceConnectorConfig restSourceConnectorConfig) {
    dataGenerator = restSourceConnectorConfig.getDataGenerator();
    this.restSourceConnectorConfig = restSourceConnectorConfig;
  }

  public void start() {

    String BASE_URI = "http://0.0.0.0:8080/garmin/";
    ServiceLocator locator = ServiceLocatorUtilities.createAndPopulateServiceLocator();

    httpServer =
        GrizzlyHttpServerFactory.createHttpServer(
            URI.create(BASE_URI), new JerseyApplication(restSourceConnectorConfig), locator);

    try {
      httpServer.start();
      dataGenerator.initialize();

      System.out.println(String.format("Jersey app started.\nHit enter to stop it...", BASE_URI));
      System.in.read();
    } catch (IOException e) {
      log.error("error starting server: " + e.getLocalizedMessage(), e);
    }
  }

  public void stop() {
    dataGenerator.stop();
    httpServer.shutdown();
  }
}
