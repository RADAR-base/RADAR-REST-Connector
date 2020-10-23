package org.radarbase.connect.rest.garmin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Stress
{
    private String userAccessToken;
    private String summaryId;
    private Integer startTimeInSeconds;
    private Integer startTimeOffsetInSeconds;
    private Integer durationInSeconds;
    private String calendarDate;
    private Integer maxStressLevel;
    private Integer averageStressLevel;
    private Map<Integer, Integer> timeOffsetStressLevelValues;
    
   
    
    public String getUserAccessToken()
    {
        return userAccessToken;
    }
    public void setUserAccessToken(String userAccessToken)
    {
        this.userAccessToken = userAccessToken;
    }
    public String getSummaryId()
    {
        return summaryId;
    }
    public void setSummaryId(String summaryId)
    {
        this.summaryId = summaryId;
    }
    public Integer getStartTimeInSeconds()
    {
        return startTimeInSeconds;
    }
    public void setStartTimeInSeconds(Integer startTimeInSeconds)
    {
        this.startTimeInSeconds = startTimeInSeconds;
    }
    public Integer getStartTimeOffsetInSeconds()
    {
        return startTimeOffsetInSeconds;
    }
    public void setStartTimeOffsetInSeconds(Integer startTimeOffsetInSeconds)
    {
        this.startTimeOffsetInSeconds = startTimeOffsetInSeconds;
    }
    public Integer getDurationInSeconds()
    {
        return durationInSeconds;
    }
    public void setDurationInSeconds(Integer durationInSeconds)
    {
        this.durationInSeconds = durationInSeconds;
    }
    public String getCalendarDate()
    {
        return calendarDate;
    }
    public void setCalendarDate(String calendarDate)
    {
        this.calendarDate = calendarDate;
    }
    public Integer getMaxStressLevel()
    {
        return maxStressLevel;
    }
    public void setMaxStressLevel(Integer maxStressLevel)
    {
        this.maxStressLevel = maxStressLevel;
    }
    public Integer getAverageStressLevel()
    {
        return averageStressLevel;
    }
    public void setAverageStressLevel(Integer averageStressLevel)
    {
        this.averageStressLevel = averageStressLevel;
    }
    public Map<Integer, Integer> getTimeOffsetStressLevelValues()
    {
        return timeOffsetStressLevelValues;
    }
    public void setTimeOffsetStressLevelValues(Map<Integer, Integer> timeOffsetStressLevelValues)
    {
        this.timeOffsetStressLevelValues = timeOffsetStressLevelValues;
    }
}
