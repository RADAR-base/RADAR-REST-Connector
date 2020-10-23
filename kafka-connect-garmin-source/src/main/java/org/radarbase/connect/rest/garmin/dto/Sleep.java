package org.radarbase.connect.rest.garmin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Sleep
{
    private String userAccessToken;
    private String summaryId;
    private Integer durationInSeconds;
    private Integer startTimeInSeconds;
    private Integer startTimeOffsetInSeconds;
    private Integer deepSleepDurationInSeconds;
    private Integer lightSleepDurationInSeconds;
    private Integer awakeDurationInSeconds;
    private Map<String, List<TimeRange>> sleepLevelsMap;
    private String validation;

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
    public Integer getDurationInSeconds()
    {
        return durationInSeconds;
    }
    public void setDurationInSeconds(Integer durationInSeconds)
    {
        this.durationInSeconds = durationInSeconds;
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
    public Integer getDeepSleepDurationInSeconds()
    {
        return deepSleepDurationInSeconds;
    }
    public void setDeepSleepDurationInSeconds(Integer deepSleepDurationInSeconds)
    {
        this.deepSleepDurationInSeconds = deepSleepDurationInSeconds;
    }
    public Integer getLightSleepDurationInSeconds()
    {
        return lightSleepDurationInSeconds;
    }
    public void setLightSleepDurationInSeconds(Integer lightSleepDurationInSeconds)
    {
        this.lightSleepDurationInSeconds = lightSleepDurationInSeconds;
    }
    public Integer getAwakeDurationInSeconds()
    {
        return awakeDurationInSeconds;
    }
    public void setAwakeDurationInSeconds(Integer awakeDurationInSeconds)
    {
        this.awakeDurationInSeconds = awakeDurationInSeconds;
    }
    public Map<String, List<TimeRange>> getSleepLevelsMap()
    {
        return sleepLevelsMap;
    }
    public void setSleepLevelsMap(Map<String, List<TimeRange>> sleepLevelsMap)
    {
        this.sleepLevelsMap = sleepLevelsMap;
    }
    public String getValidation()
    {
        return validation;
    }
    public void setValidation(String validation)
    {
        this.validation = validation;
    }
}
