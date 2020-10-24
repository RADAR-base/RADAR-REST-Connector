package org.radarbase.connect.rest.garmin.resource;

import java.io.IOException;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.radarbase.connect.rest.garmin.dto.EventPushBody;
import org.radarbase.connect.rest.garmin.dto.GarminData;
import org.radarbase.connect.rest.garmin.service.GarminHealthApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Consumes(MediaType.APPLICATION_JSON)
@Singleton
@Path("/health-api/")
public class GarminHealthApiResource {

  private static final Logger logger = LoggerFactory.getLogger(GarminHealthApiResource.class);
  private final GarminHealthApiService healthApiService;

  @Inject
  public GarminHealthApiResource(GarminHealthApiService healthApiService) {
    this.healthApiService = healthApiService;
  }

  @POST
  @Path("/activities")
  public Response addActivities(EventPushBody pushBody) {
    if (pushBody.getActivities() == null) {
      logger.warn("Activity data was null. Returning 400");
      return Response.status(Status.BAD_REQUEST).build();
    }
    return this.addData(pushBody.getActivities());
  }

  private Response addData(List<? extends GarminData> garminData) {
    try {
      this.healthApiService.handleGarminData(garminData);
      return Response.status(Status.OK).build();
    } catch (IOException e) {
      logger.warn("I/O Exception occurred when processing the data", e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    } catch (IllegalStateException e) {
      logger.warn("Illegal State Exception occurred when processing the data", e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }
}
