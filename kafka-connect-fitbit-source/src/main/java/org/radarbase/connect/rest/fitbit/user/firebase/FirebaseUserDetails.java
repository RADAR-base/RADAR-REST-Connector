package org.radarbase.connect.rest.fitbit.user.firebase;

import com.google.cloud.firestore.annotation.IgnoreExtraProperties;
import com.google.cloud.firestore.annotation.PropertyName;

/**
 * POJO corresponding to the User details document in Firestore. Currently, we only need projectId
 * but can be expanded in the future.
 */
@IgnoreExtraProperties
public class FirebaseUserDetails {

  protected static final String DEFAULT_PROJECT_ID = "radar-firebase-default-project";

  private String projectId;

  public FirebaseUserDetails() {
    this.projectId = DEFAULT_PROJECT_ID;
  }

  @PropertyName("project_id")
  public String getProjectId() {
    return projectId;
  }

  @PropertyName("project_id")
  public void setProjectId(String projectId) {
    if (projectId != null && !projectId.trim().isEmpty()) {
      this.projectId = projectId;
    }
  }
}
