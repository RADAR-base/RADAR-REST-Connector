package org.radarbase.connect.rest.garmin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Daily
{
    private String userAccessToken;
    private String summaryId;
    private String activityType;
    private Integer activeKilocalories;
    private Integer bmrKilocalories;
    private Integer steps;
    private Float distanceInMeters;
    private Integer durationInSeconds;
    private Integer activeTimeInSeconds;
    private Integer startTimeInSeconds;
    private Integer startTimeOffsetInSeconds;
    private Integer moderateIntensityDurationInSeconds;
    private Integer vigorousIntensityDurationInSeconds;
    private Integer floorsClimbed;
    private Integer minHeartRateInBeatsPerMinute;
    private Integer maxHeartRateInBeatsPerMinute;
    private Integer averageHeartRateInBeatsPerMinute;
    private Integer restingHeartRateInBeatsPerMinute;
    private Map<Integer, Short> timeOffsetHeartRateSamples;
    // populated for third-party sources like FITBIT
    private String source;
    private Integer stepsGoal;
    private Integer netKilocaloriesGoal;
    private Integer intensityDurationGoalInSeconds;
    private Integer floorsClimbedGoal;
    private Integer averageStressLevel;
    private Integer maxStressLevel;
    private Integer stressDurationInSeconds;
    private Integer restStressDurationInSeconds;
    private Integer activityStressDurationInSeconds;
    private Integer lowStressDurationInSeconds;
    private Integer mediumStressDurationInSeconds;
    private Integer highStressDurationInSeconds;
    private String stressQualifier;
    
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
    public String getActivityType()
    {
        return activityType;
    }
    public void setActivityType(String activityType)
    {
        this.activityType = activityType;
    }
    public Integer getActiveKilocalories()
    {
        return activeKilocalories;
    }
    public void setActiveKilocalories(Integer activeKilocalories)
    {
        this.activeKilocalories = activeKilocalories;
    }
    public Integer getBmrKilocalories()
    {
        return bmrKilocalories;
    }
    public void setBmrKilocalories(Integer bmrKilocalories)
    {
        this.bmrKilocalories = bmrKilocalories;
    }
    public Integer getSteps()
    {
        return steps;
    }
    public void setSteps(Integer steps)
    {
        this.steps = steps;
    }
    public Float getDistanceInMeters()
    {
        return distanceInMeters;
    }
    public void setDistanceInMeters(Float distanceInMeters)
    {
        this.distanceInMeters = distanceInMeters;
    }
    public Integer getDurationInSeconds()
    {
        return durationInSeconds;
    }
    public void setDurationInSeconds(Integer durationInSeconds)
    {
        this.durationInSeconds = durationInSeconds;
    }
    public Integer getActiveTimeInSeconds()
    {
        return activeTimeInSeconds;
    }
    public void setActiveTimeInSeconds(Integer activeTimeInSeconds)
    {
        this.activeTimeInSeconds = activeTimeInSeconds;
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
    public Integer getModerateIntensityDurationInSeconds()
    {
        return moderateIntensityDurationInSeconds;
    }
    public void setModerateIntensityDurationInSeconds(Integer moderateIntensityDurationInSeconds)
    {
        this.moderateIntensityDurationInSeconds = moderateIntensityDurationInSeconds;
    }
    public Integer getVigorousIntensityDurationInSeconds()
    {
        return vigorousIntensityDurationInSeconds;
    }
    public void setVigorousIntensityDurationInSeconds(Integer vigorousIntensityDurationInSeconds)
    {
        this.vigorousIntensityDurationInSeconds = vigorousIntensityDurationInSeconds;
    }
    public Integer getFloorsClimbed()
    {
        return floorsClimbed;
    }
    public void setFloorsClimbed(Integer floorsClimbed)
    {
        this.floorsClimbed = floorsClimbed;
    }
    public Integer getMinHeartRateInBeatsPerMinute()
    {
        return minHeartRateInBeatsPerMinute;
    }
    public void setMinHeartRateInBeatsPerMinute(Integer minHeartRateInBeatsPerMinute)
    {
        this.minHeartRateInBeatsPerMinute = minHeartRateInBeatsPerMinute;
    }
    public Integer getMaxHeartRateInBeatsPerMinute()
    {
        return maxHeartRateInBeatsPerMinute;
    }
    public void setMaxHeartRateInBeatsPerMinute(Integer maxHeartRateInBeatsPerMinute)
    {
        this.maxHeartRateInBeatsPerMinute = maxHeartRateInBeatsPerMinute;
    }
    public Integer getAverageHeartRateInBeatsPerMinute()
    {
        return averageHeartRateInBeatsPerMinute;
    }
    public void setAverageHeartRateInBeatsPerMinute(Integer averageHeartRateInBeatsPerMinute)
    {
        this.averageHeartRateInBeatsPerMinute = averageHeartRateInBeatsPerMinute;
    }
    public Integer getRestingHeartRateInBeatsPerMinute()
    {
        return restingHeartRateInBeatsPerMinute;
    }
    public void setRestingHeartRateInBeatsPerMinute(final Integer restingHeartRateInBeatsPerMinute)
    {
        this.restingHeartRateInBeatsPerMinute = restingHeartRateInBeatsPerMinute;
    }
    public Map<Integer, Short> getTimeOffsetHeartRateSamples()
    {
        return timeOffsetHeartRateSamples;
    }
    public void setTimeOffsetHeartRateSamples(Map<Integer, Short> timeOffsetHeartRateSamples)
    {
        this.timeOffsetHeartRateSamples = timeOffsetHeartRateSamples;
    }
    public String getSource()
    {
        return source;
    }
    public void setSource(String source)
    {
        this.source = source;
    }
    public Integer getStepsGoal()
    {
        return stepsGoal;
    }
    public void setStepsGoal(Integer stepsGoal)
    {
        this.stepsGoal = stepsGoal;
    }
    public Integer getNetKilocaloriesGoal()
    {
        return netKilocaloriesGoal;
    }
    public void setNetKilocaloriesGoal(Integer netKilocaloriesGoal)
    {
        this.netKilocaloriesGoal = netKilocaloriesGoal;
    }
    public Integer getIntensityDurationGoalInSeconds()
    {
        return intensityDurationGoalInSeconds;
    }
    public void setIntensityDurationGoalInSeconds(Integer intensityDurationGoalInSeconds)
    {
        this.intensityDurationGoalInSeconds = intensityDurationGoalInSeconds;
    }
    public Integer getFloorsClimbedGoal()
    {
        return floorsClimbedGoal;
    }
    public void setFloorsClimbedGoal(Integer floorsClimbedGoal)
    {
        this.floorsClimbedGoal = floorsClimbedGoal;
    }
    public Integer getAverageStressLevel()
    {
        return averageStressLevel;
    }
    public void setAverageStressLevel(Integer averageStressLevel)
    {
        this.averageStressLevel = averageStressLevel;
    }
    public Integer getMaxStressLevel()
    {
        return maxStressLevel;
    }
    public void setMaxStressLevel(Integer maxStressLevel)
    {
        this.maxStressLevel = maxStressLevel;
    }
    public Integer getStressDurationInSeconds()
    {
        return stressDurationInSeconds;
    }
    public void setStressDurationInSeconds(Integer stressDurationInSeconds)
    {
        this.stressDurationInSeconds = stressDurationInSeconds;
    }
    public Integer getRestStressDurationInSeconds()
    {
        return restStressDurationInSeconds;
    }
    public void setRestStressDurationInSeconds(Integer restStressDurationInSeconds)
    {
        this.restStressDurationInSeconds = restStressDurationInSeconds;
    }
    public Integer getActivityStressDurationInSeconds()
    {
        return activityStressDurationInSeconds;
    }
    public void setActivityStressDurationInSeconds(Integer activityStressDurationInSeconds)
    {
        this.activityStressDurationInSeconds = activityStressDurationInSeconds;
    }
    public Integer getLowStressDurationInSeconds()
    {
        return lowStressDurationInSeconds;
    }
    public void setLowStressDurationInSeconds(Integer lowStressDurationInSeconds)
    {
        this.lowStressDurationInSeconds = lowStressDurationInSeconds;
    }
    public Integer getMediumStressDurationInSeconds()
    {
        return mediumStressDurationInSeconds;
    }
    public void setMediumStressDurationInSeconds(Integer mediumStressDurationInSeconds)
    {
        this.mediumStressDurationInSeconds = mediumStressDurationInSeconds;
    }
    public Integer getHighStressDurationInSeconds()
    {
        return highStressDurationInSeconds;
    }
    public void setHighStressDurationInSeconds(Integer highStressDurationInSeconds)
    {
        this.highStressDurationInSeconds = highStressDurationInSeconds;
    }
    public String getStressQualifier()
    {
        return stressQualifier;
    }
    public void setStressQualifier(String stressQualifier)
    {
        this.stressQualifier = stressQualifier;
    }
}
