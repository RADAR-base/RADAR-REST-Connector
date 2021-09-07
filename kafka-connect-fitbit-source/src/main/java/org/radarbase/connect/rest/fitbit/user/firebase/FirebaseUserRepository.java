package org.radarbase.connect.rest.fitbit.user.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.SetOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for Firebase User repositories. Initialises the Firebase Admin SDK for transactions
 * with Firebase.
 */
public abstract class FirebaseUserRepository implements UserRepository {

  private static final Logger logger = LoggerFactory.getLogger(FirebaseUserRepository.class);

  @Override
  public void initialize(RestSourceConnectorConfig config) {

    // The path to the credentials file should be provided by
    // the GOOGLE_APPLICATION_CREDENTIALS env var.
    // See https://firebase.google.com/docs/admin/setup#initialize-sdk for more details.
    FirebaseOptions options;
    try {
      options = FirebaseOptions.builder()
          .setCredentials(GoogleCredentials.getApplicationDefault())
          .build();
    } catch (IOException exc) {
      logger.error("Failed to get credentials for Firebase app.", exc);
      throw new IllegalStateException(exc);
    }

    try {
      FirebaseApp.initializeApp(options);
    } catch (IllegalStateException exc) {
      logger.warn("Firebase app was already initialised. {}", exc.getMessage());
    }
  }

  protected Firestore getFirestore() {
    return FirestoreClient.getFirestore();
  }

  /**
   * Get a document from a Collection in Firestore.
   *
   * @param key The document ID to pull from the collection
   * @param collection The collection reference to query
   * @return the document
   * @throws IOException If there was a problem getting the document
   */
  protected DocumentSnapshot getDocument(String key, CollectionReference collection)
      throws IOException {
    DocumentSnapshot documentSnapshot;
    try {
      documentSnapshot = collection.document(key).get().get(20, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new IOException(e);
    }
    return documentSnapshot;
  }

  /**
   * Writes the specified object to a Firestore document.
   *
   * @param documentReference document reference for the document to be updated
   * @param object The POJO to write to the document
   * @throws IOException If there was a problem updating the document
   */
  protected void updateDocument(DocumentReference documentReference, Object object)
      throws IOException {
    try {
      documentReference.set(object, SetOptions.merge()).get(20, TimeUnit.SECONDS);
    } catch (InterruptedException | TimeoutException | ExecutionException e) {
      throw new IOException(e);
    }
  }
}
