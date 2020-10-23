package org.radarbase.connect.rest.garmin.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserInfo 
{
    private String userAccessToken;
    private String summaryId;
    private String userId;
    private String gender;
    private Integer heightInCentimeters;
    private Integer weightInGrams;
    private String timeZone;
    private Integer modifiedTimeInSeconds;


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

    public String getUserId()
    {
        return userId;
    }

    public void setUserId(final String userId)
    {
        this.userId = userId;
    }

    public String getGender()
    {
        return gender;
    }

    public void setGender(final String gender)
    {
        this.gender = gender;
    }

    public Integer getHeightInCentimeters()
    {
        return heightInCentimeters;
    }

    public void setHeightInCentimeters(final Integer heightInCentimeters)
    {
        this.heightInCentimeters = heightInCentimeters;
    }

    public Integer getWeightInGrams()
    {
        return weightInGrams;
    }

    public void setWeightInGrams(final Integer weightInGrams)
    {
        this.weightInGrams = weightInGrams;
    }

    public String getTimeZone()
    {
        return timeZone;
    }

    public void setTimeZone(final String timeZone)
    {
        this.timeZone = timeZone;
    }

    public Integer getModifiedTimeInSeconds()
    {
        return modifiedTimeInSeconds;
    }

    public void setModifiedTimeInSeconds(final Integer modifiedTimeInSeconds)
    {
        this.modifiedTimeInSeconds = modifiedTimeInSeconds;
    }
}
