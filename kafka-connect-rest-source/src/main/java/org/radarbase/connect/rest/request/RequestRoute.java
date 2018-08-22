package org.radarbase.connect.rest.request;

import okhttp3.Response;
import org.apache.kafka.connect.source.SourceRecord;
import org.radarbase.connect.rest.converter.PayloadToSourceRecordConverter;

/** Single request route. This may represent e.g. a URL. */
public interface RequestRoute extends RequestGenerator {
  /** Data converter from data that is returned from the route. */
  PayloadToSourceRecordConverter converter();

  /**
   * Called when the request from this route succeeded.
   *
   * @param request non-null generated request
   * @param record non-null resulting record
   */
  void requestSucceeded(RestRequest request, SourceRecord record);
  void requestEmpty(RestRequest request);
  void requestFailed(RestRequest request, Response response);
}
