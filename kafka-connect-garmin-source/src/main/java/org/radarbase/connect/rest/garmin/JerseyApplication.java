package org.radarbase.connect.rest.garmin;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import javax.inject.Singleton;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.garmin.generator.DataGenerator;
import org.radarbase.connect.rest.garmin.util.ObjectMapperFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JerseyApplication extends ResourceConfig {

  private static final Logger log = LoggerFactory.getLogger(JerseyApplication.class);

  public JerseyApplication(GarminRestSourceConnectorConfig restSourceConnectorConfig) {
    log.info("setting up hk2");
    packages(
        "org.radarbase.connect.rest.garmin.dto",
        "org.radarbase.connect.rest.garmin.controller",
        "org.radarbase.connect.rest.garmin.service",
        "org.radarbase.connect.rest.garmin.filter");

    JacksonJaxbJsonProvider jacksonJaxbJsonProvider = new JacksonJaxbJsonProvider();
    jacksonJaxbJsonProvider.setMapper(ObjectMapperFactory.buildObjectMapper());
    register(jacksonJaxbJsonProvider);
    register(
        new AbstractBinder() {
          @Override
          protected void configure() {

            bind(restSourceConnectorConfig.getDataGenerator())
                .to(DataGenerator.class)
                .in(Singleton.class);
            bind(restSourceConnectorConfig).to(RestSourceConnectorConfig.class);
            // Shop to manually bind objects, in the case that the Jersey Auto-scan isn't working
            // e.g. bind(x.class).to(y.class).in(Singleton.class);
            // e.g. bind(x.class).to(y.class);
            //
            // note: if the object is generic, use TypeLiteral
            // e.g. bind(x.class).to(new TypeLiteral&lt;InjectionResolver&gt;(){});
            //
          }
        });
  }
}
