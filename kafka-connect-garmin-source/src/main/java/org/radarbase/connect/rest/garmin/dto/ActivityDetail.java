package org.radarbase.connect.rest.garmin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActivityDetail 
{
    private String userAccessToken;
    private String summaryId;
    private com.garmin.gh.apps.wellnessmonitor.domain.eventpush.Activity summary;
    private List<Sample> samples;
    
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
    public com.garmin.gh.apps.wellnessmonitor.domain.eventpush.Activity getSummary()
    {
        return summary;
    }
    public void setSummary(com.garmin.gh.apps.wellnessmonitor.domain.eventpush.Activity summary)
    {
        this.summary = summary;
    }
    public List<Sample> getSamples()
    {
        return samples;
    }
    public void setSamples(List<Sample> samples)
    {
        this.samples = samples;
    }
       
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Sample
    {
        private Integer startTimeInSeconds;
        private Double latitudeInDegree;
        private Double longitudeInDegree;
        private Double elevationInMeters;
        private Double airTemperatureCelcius;
        private Integer heartRate;
        private Double speedMetersPerSecond;
        private Double stepsPerMinute;
        private Double totalDistanceInMeters;
        private Integer timerDurationInSeconds;
        private Integer clockDurationInSeconds;
        private Integer movingDurationInSeconds;
        
        public Integer getStartTimeInSeconds()
        {
            return startTimeInSeconds;
        }
        public void setStartTimeInSeconds(Integer startTimeInSeconds)
        {
            this.startTimeInSeconds = startTimeInSeconds;
        }
        public Double getLatitudeInDegree()
        {
            return latitudeInDegree;
        }
        public void setLatitudeInDegree(Double latitudeInDegree)
        {
            this.latitudeInDegree = latitudeInDegree;
        }
        public Double getLongitudeInDegree()
        {
            return longitudeInDegree;
        }
        public void setLongitudeInDegree(Double longitudeInDegree)
        {
            this.longitudeInDegree = longitudeInDegree;
        }
        public Double getElevationInMeters()
        {
            return elevationInMeters;
        }
        public void setElevationInMeters(Double elevationInMeters)
        {
            this.elevationInMeters = elevationInMeters;
        }
        public Double getAirTemperatureCelcius()
        {
            return airTemperatureCelcius;
        }
        public void setAirTemperatureCelcius(Double airTemperatureCelcius)
        {
            this.airTemperatureCelcius = airTemperatureCelcius;
        }
        public Integer getHeartRate()
        {
            return heartRate;
        }
        public void setHeartRate(Integer heartRate)
        {
            this.heartRate = heartRate;
        }
        public Double getSpeedMetersPerSecond()
        {
            return speedMetersPerSecond;
        }
        public void setSpeedMetersPerSecond(Double speedMetersPerSecond)
        {
            this.speedMetersPerSecond = speedMetersPerSecond;
        }
        public Double getStepsPerMinute()
        {
            return stepsPerMinute;
        }
        public void setStepsPerMinute(Double stepsPerMinute)
        {
            this.stepsPerMinute = stepsPerMinute;
        }
        public Double getTotalDistanceInMeters()
        {
            return totalDistanceInMeters;
        }
        public void setTotalDistanceInMeters(Double totalDistanceInMeters)
        {
            this.totalDistanceInMeters = totalDistanceInMeters;
        }
        public Integer getTimerDurationInSeconds()
        {
            return timerDurationInSeconds;
        }
        public void setTimerDurationInSeconds(Integer timerDurationInSeconds)
        {
            this.timerDurationInSeconds = timerDurationInSeconds;
        }
        public Integer getClockDurationInSeconds()
        {
            return clockDurationInSeconds;
        }
        public void setClockDurationInSeconds(Integer clockDurationInSeconds)
        {
            this.clockDurationInSeconds = clockDurationInSeconds;
        }
        public Integer getMovingDurationInSeconds()
        {
            return movingDurationInSeconds;
        }
        public void setMovingDurationInSeconds(Integer movingDurationInSeconds)
        {
            this.movingDurationInSeconds = movingDurationInSeconds;
        }
        
    }
   
}
