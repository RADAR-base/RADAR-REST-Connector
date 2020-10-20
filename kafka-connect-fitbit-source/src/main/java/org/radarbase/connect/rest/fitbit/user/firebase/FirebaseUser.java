package org.radarbase.connect.rest.fitbit.user.firebase;

import io.confluent.connect.avro.AvroData;
import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.radarbase.connect.rest.fitbit.user.User;

/**
 * {@link User} implementation corresponding with how users are stored in Firestore. The fitbit
 * details are stored in one collection while user details in another collection. This combines the
 * data from the two to form the User instance.
 */
public class FirebaseUser implements User {

  protected static final Pattern ILLEGAL_CHARACTERS_PATTERN = Pattern.compile("[^a-zA-Z0-9_-]");
  private String uuid;
  private String userId;

  private FirebaseUserDetails firebaseUserDetails;
  private FirebaseFitbitAuthDetails fitbitAuthDetails;

  private SchemaAndValue observationKey;

  public FirebaseUser() {
    firebaseUserDetails = new FirebaseUserDetails();
    fitbitAuthDetails = new FirebaseFitbitAuthDetails();
  }

  public String getId() {
    return this.uuid;
  }

  public String getVersion() {
    return fitbitAuthDetails.getVersion();
  }

  public void setUuid(String uuid) {
    this.uuid = ILLEGAL_CHARACTERS_PATTERN.matcher(uuid).replaceAll("-");
  }

  public String getExternalUserId() {
    return fitbitAuthDetails.getOauth2Credentials() == null
        ? null
        : fitbitAuthDetails.getOauth2Credentials().getUserId();
  }

  public String getProjectId() {
    return firebaseUserDetails.getProjectId();
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public Instant getStartDate() {
    return fitbitAuthDetails.getStartDate().toInstant();
  }

  public Instant getEndDate() {
    return fitbitAuthDetails.getEndDate().toInstant();
  }

  public String getSourceId() {
    return fitbitAuthDetails.getSourceId();
  }

  @Override
  public boolean isAuthorized() {
    return !fitbitAuthDetails.getOauth2Credentials().isAccessTokenExpired()
        || fitbitAuthDetails.getOauth2Credentials().hasRefreshToken();
  }

  public FirebaseUserDetails getFirebaseUserDetails() {
    return firebaseUserDetails;
  }

  public void setFirebaseUserDetails(FirebaseUserDetails firebaseUserDetails) {
    this.firebaseUserDetails = firebaseUserDetails;
  }

  public FirebaseFitbitAuthDetails getFitbitAuthDetails() {
    return fitbitAuthDetails;
  }

  public void setFitbitAuthDetails(FirebaseFitbitAuthDetails fitbitAuthDetails) {
    this.fitbitAuthDetails = fitbitAuthDetails;
  }

  public synchronized SchemaAndValue getObservationKey(AvroData avroData) {
    if (observationKey == null) {
      observationKey = User.computeObservationKey(avroData, this);
    }
    return observationKey;
  }

  @Override
  public String toString() {
    return "FirebaseUser{"
        + "uuid='"
        + uuid
        + '\''
        + ", fitbitUserId='"
        + getExternalUserId()
        + '\''
        + ", projectId='"
        + getProjectId()
        + '\''
        + ", userId='"
        + userId
        + '\''
        + ", sourceId='"
        + fitbitAuthDetails.getSourceId()
        + '\''
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FirebaseUser firebaseUser = (FirebaseUser) o;
    return Objects.equals(uuid, firebaseUser.uuid)
        && Objects.equals(getExternalUserId(), firebaseUser.getExternalUserId())
        && Objects.equals(getProjectId(), firebaseUser.getProjectId())
        && Objects.equals(userId, firebaseUser.userId)
        && Objects.equals(getSourceId(), firebaseUser.getSourceId())
        && Objects.equals(getStartDate(), firebaseUser.getStartDate())
        && Objects.equals(getEndDate(), firebaseUser.getEndDate())
        && Objects.equals(observationKey, firebaseUser.observationKey);
  }

  @Override
  public int hashCode() {

    return Objects.hash(
        uuid,
        getExternalUserId(),
        getProjectId(),
        userId,
        getSourceId(),
        getStartDate(),
        getEndDate(),
        observationKey);
  }
}
