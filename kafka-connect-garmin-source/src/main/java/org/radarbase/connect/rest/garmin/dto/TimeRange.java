package org.radarbase.connect.rest.garmin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TimeRange
{
    private Integer startTimeInSeconds;
    private Integer endTimeInSeconds;
    
    public Integer getStartTimeInSeconds()
    {
        return startTimeInSeconds;
    }
    public void setStartTimeInSeconds(Integer startTimeInSeconds)
    {
        this.startTimeInSeconds = startTimeInSeconds;
    }
    public Integer getEndTimeInSeconds()
    {
        return endTimeInSeconds;
    }
    public void setEndTimeInSeconds(Integer endTimeInSeconds)
    {
        this.endTimeInSeconds = endTimeInSeconds;
    }
}
