package org.radarbase.connect.rest.fitbit.user.firebase;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentChange;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.EventListener;
import com.google.cloud.firestore.FirestoreException;
import com.google.cloud.firestore.ListenerRegistration;
import com.google.cloud.firestore.QuerySnapshot;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.ws.rs.NotAuthorizedException;
import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.FitbitRestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The User repository that supports the covid-collab application (https://covid-collab.org/). The
 * data is stored in Firebase Firestore. This user repository reads data from this source and
 * creates user objects. To ease the creation of objects a new {@link User} {@link FirebaseUser} is
 * created.
 *
 * <p>Structure in Firestore is :- 1) Fitbit Collection -> User Document(uuid) -> Fitbit Details. 2)
 * Users Collection -> User Document(uuid) -> User Details.
 *
 * <p>See {@link FirebaseFitbitAuthDetails} for the keys present in Ftibit Details for each User.
 * See {@link FirebaseUserDetails} for the keys present in User Details for each User.
 */
public class CovidCollabFirebaseUserRepository extends FirebaseUserRepository {

  protected static final String FITBIT_TOKEN_ENDPOINT = "https://api.fitbit.com/oauth2/token";
  private static final Logger logger =
      LoggerFactory.getLogger(CovidCollabFirebaseUserRepository.class);

  private final Map<String, FirebaseUser> cachedUsers = new HashMap<>();
  private CollectionReference userCollection;
  private CollectionReference fitbitCollection;
  private FitbitTokenService fitbitTokenService;
  private List<String> allowedUsers;
  private ListenerRegistration fitbitCollectionListenerRegistration;
  private boolean hasPendingUpdates = true;

  @Override
  public User get(String key) throws IOException {
    return this.cachedUsers.getOrDefault(key, createUser(key));
  }

  @Override
  public Stream<? extends User> stream() {
    return cachedUsers.values().stream();
  }

  @Override
  public String getAccessToken(User user) throws IOException, NotAuthorizedException {
    FitbitOAuth2UserCredentials credentials =
        cachedUsers.get(user.getId()).getFitbitAuthDetails().getOauth2Credentials();
    if (credentials == null || credentials.isAccessTokenExpired()) {
      return refreshAccessToken(user);
    }
    return credentials.getAccessToken();
  }

  @Override
  public String refreshAccessToken(User user) throws IOException, NotAuthorizedException {
    FirebaseFitbitAuthDetails authDetails = cachedUsers.get(user.getId()).getFitbitAuthDetails();

    logger.debug("Refreshing token for User: {}", cachedUsers.get(user.getId()));
    if (!authDetails.getOauth2Credentials().hasRefreshToken()) {
      logger.error("No refresh Token present");
      throw new NotAuthorizedException("The user does not contain a refresh token");
    }

    // Make call to fitbit to get new refresh and access token.
    logger.info("Requesting to refreshToken.");
    FitbitOAuth2UserCredentials userCredentials =
        fitbitTokenService.refreshToken(authDetails.getOauth2Credentials().getRefreshToken());
    logger.debug("Token Refreshed.");

    if (userCredentials.hasRefreshToken() && userCredentials.getAccessToken() != null) {
      authDetails.setOauth2Credentials(userCredentials);
      updateDocument(fitbitCollection.document(user.getId()), authDetails);
      this.cachedUsers.get(user.getId()).setFitbitAuthDetails(authDetails);
      return userCredentials.getAccessToken();
    } else {
      throw new IOException("There was a problem refreshing the token.");
    }
  }

  @Override
  public boolean hasPendingUpdates() {
    return this.hasPendingUpdates;
  }

  @Override
  public void applyPendingUpdates() throws IOException {
    if (this.hasPendingUpdates()) {
      this.hasPendingUpdates = false;
    } else {
      throw new IOException(
          "No pending updates available. Try calling this method only when updates are available");
    }
  }

  @Override
  public void initialize(RestSourceConnectorConfig config) {
    super.initialize(config);

    FitbitRestSourceConnectorConfig fitbitConfig = (FitbitRestSourceConnectorConfig) config;
    this.fitbitCollection =
        getFirestore().collection(fitbitConfig.getFitbitUserRepositoryFirestoreFitbitCollection());
    this.userCollection =
        getFirestore().collection(fitbitConfig.getFitbitUserRepositoryFirestoreUserCollection());

    this.fitbitTokenService =
        new FitbitTokenService(
            fitbitConfig.getFitbitClient(),
            fitbitConfig.getFitbitClientSecret(),
            FITBIT_TOKEN_ENDPOINT);

    /*
     * Currently, we only listen for the fitbit collection, as it contains most information while
     * the user collection only contains project Id which is not supposed to change. The user
     * document is pulled every time the corresponding fitbit document is pulled, so it will be
     * sufficiently upto date. Moreover, not every document in the user collection will have linked
     * the fitbit. In the future, we might listen to user collection too if required.
     */
    if (this.fitbitCollectionListenerRegistration == null) {
      this.fitbitCollectionListenerRegistration = initListener(fitbitCollection, this::onEvent);
      logger.info("Added listener to Fitbit collection for real-time updates.");
    }

    this.allowedUsers = fitbitConfig.getFitbitUsers();
  }

  private ListenerRegistration initListener(
      CollectionReference collectionReference, EventListener<QuerySnapshot> eventListener) {
    return collectionReference.addSnapshotListener(eventListener);
  }

  protected FirebaseUser createUser(String uuid) throws IOException {
    DocumentSnapshot fitbitDocumentSnapshot = getDocument(uuid, fitbitCollection);
    DocumentSnapshot userDocumentSnapshot = getDocument(uuid, userCollection);

    return createUser(userDocumentSnapshot, fitbitDocumentSnapshot);
  }

  protected FirebaseUser createUser(
      DocumentSnapshot userSnapshot, DocumentSnapshot fitbitSnapshot) {
    // Get the fitbit document for the user which contains Auth Info
    FirebaseFitbitAuthDetails authDetails =
        fitbitSnapshot.toObject(FirebaseFitbitAuthDetails.class);
    // Get the user document for the user which contains User Details
    FirebaseUserDetails userDetails = userSnapshot.toObject(FirebaseUserDetails.class);

    logger.debug("Auth details: {}", authDetails);
    logger.debug("User Details: {}", userDetails);

    // if auth details are not available, skip this user.
    if (authDetails == null || authDetails.getOauth2Credentials() == null) {
      logger.warn(
          "The auth details for user {} in the database are not valid. Skipping...",
          fitbitSnapshot.getId());
      return null;
    }

    // If no user details found, create one with default project.
    if (userDetails == null) {
      userDetails = new FirebaseUserDetails();
    }

    FirebaseUser user = new FirebaseUser();
    user.setUuid(fitbitSnapshot.getId());
    user.setUserId(fitbitSnapshot.getId());
    user.setFitbitAuthDetails(authDetails);
    user.setFirebaseUserDetails(userDetails);
    return user;
  }

  private void updateUser(DocumentSnapshot fitbitDocumentSnapshot) {
    try {
      FirebaseUser user =
          createUser(
              getDocument(fitbitDocumentSnapshot.getId(), userCollection), fitbitDocumentSnapshot);
      logger.debug("User to be updated: {}", user);
      if (user != null
          && user.isComplete()
          && (allowedUsers.isEmpty() || allowedUsers.contains(user.getId()))) {
        FirebaseUser user1 = this.cachedUsers.put(fitbitDocumentSnapshot.getId(), user);
        if (user1 == null) {
          logger.info("Created new User: {}", fitbitDocumentSnapshot.getId());
        } else {
          logger.info("Updated existing user: {}", user1);
          logger.debug("Updated user is: {}", user);
        }
        this.hasPendingUpdates = true;
      } else {
        removeUser(fitbitDocumentSnapshot);
      }
    } catch (IOException e) {
      logger.error(
          "The update of the user {} was not possible.", fitbitDocumentSnapshot.getId(), e);
    }
  }

  private void removeUser(DocumentSnapshot documentSnapshot) {
    FirebaseUser user = this.cachedUsers.remove(documentSnapshot.getId());
    if (user != null) {
      logger.info("Removed User: {}:", user);
      this.hasPendingUpdates = true;
    }
  }

  private void onEvent(QuerySnapshot snapshots, FirestoreException e) {
    if (e != null) {
      logger.warn("Listen for updates failed: " + e);
      return;
    }

    logger.debug(
        "OnEvent Called: {}, {}",
        snapshots.getDocumentChanges().size(),
        snapshots.getDocuments().size());
    for (DocumentChange dc : snapshots.getDocumentChanges()) {
      logger.debug("Type: {}", dc.getType());
      switch (dc.getType()) {
        case ADDED:
        case MODIFIED:
          this.updateUser(dc.getDocument());
          break;
        case REMOVED:
          this.removeUser(dc.getDocument());
        default:
          break;
      }
    }
  }
}
