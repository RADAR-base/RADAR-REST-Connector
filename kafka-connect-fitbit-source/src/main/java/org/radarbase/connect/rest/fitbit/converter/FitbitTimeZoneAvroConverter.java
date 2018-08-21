package org.radarbase.connect.rest.fitbit.converter;

    import com.fasterxml.jackson.databind.JsonNode;
    import io.confluent.connect.avro.AvroData;
    import java.util.stream.Stream;
    import org.radarbase.connect.rest.RestSourceConnectorConfig;
    import org.radarbase.connect.rest.fitbit.FitbitRestSourceConnectorConfig;
    import org.radarbase.connect.rest.fitbit.request.FitbitRestRequest;
    import org.radarcns.connector.fitbit.FitbitTimeZone;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;

public class FitbitTimeZoneAvroConverter extends FitbitAvroConverter {
  private static final Logger logger = LoggerFactory.getLogger(FitbitTimeZoneAvroConverter.class);

  private String timeZoneTopic;

  public FitbitTimeZoneAvroConverter(AvroData avroData) {
    super(avroData);
  }

  @Override
  public void initialize(RestSourceConnectorConfig config) {
    timeZoneTopic = ((FitbitRestSourceConnectorConfig)config).getFitbitTimeZoneTopic();
    logger.info("Using timezone topic {}", timeZoneTopic);
  }

  @Override
  protected Stream<TopicData> processRecords(FitbitRestRequest request, JsonNode root,
      double timeReceived) {
    JsonNode user = root.get("user");
    if (user == null) {
      logger.warn("Failed to get timezone from {}, {}", request.getRequest().url(), root);
      return Stream.empty();
    }
    JsonNode offsetNode = user.get("offsetFromUTCMillis");
    Integer offset = offsetNode == null ? null : (int) (offsetNode.asLong() / 1000L);

    FitbitTimeZone timeZone = new FitbitTimeZone(timeReceived, offset);

    return Stream.of(new TopicData(request.getStartOffset(), timeZoneTopic, timeZone));
  }
}
