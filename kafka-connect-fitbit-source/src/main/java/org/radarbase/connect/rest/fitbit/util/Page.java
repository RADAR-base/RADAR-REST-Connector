package org.radarbase.connect.rest.fitbit.util;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Page {

  public int pageNumber;
  public int pageSize;
  public long totalElements;
  public int offset;

  public Page(@JsonProperty("pageNumber") int pageNumber,
              @JsonProperty("pageSize") int pageSize,
              @JsonProperty("totalElements") long totalElements,
              @JsonProperty("offset") int offset) {
    this.pageNumber = pageNumber;
    this.pageSize = pageSize;
    this.totalElements = totalElements;
    this.offset = offset;
  }

}
