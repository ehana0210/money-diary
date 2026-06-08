package com.moneydiary.api.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * Initializes the Firebase Admin SDK once and exposes Firestore and
 * FirebaseAuth as Spring beans.
 *
 * <p>Credentials are resolved via Application Default Credentials (ADC):
 * <ul>
 *   <li>On Cloud Run the runtime service account is used automatically.</li>
 *   <li>Locally, set GOOGLE_APPLICATION_CREDENTIALS to a service account key,
 *       or run {@code gcloud auth application-default login}.</li>
 * </ul>
 */
@Configuration
public class FirebaseConfig {

    @Value("${gcp.project-id:}")
    private String projectId;

    /**
     * Firestore 데이터베이스 ID. named 데이터베이스를 쓰면 그 이름을, 기본 DB 면 "(default)".
     */
    @Value("${gcp.firestore.database-id:(default)}")
    private String databaseId;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        FirebaseOptions.Builder builder = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.getApplicationDefault());

        if (StringUtils.hasText(projectId)) {
            builder.setProjectId(projectId);
        }

        return FirebaseApp.initializeApp(builder.build());
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }

    @Bean
    public Firestore firestore() throws IOException {
        FirestoreOptions.Builder builder = FirestoreOptions.newBuilder()
                .setCredentials(GoogleCredentials.getApplicationDefault());

        if (StringUtils.hasText(projectId)) {
            builder.setProjectId(projectId);
        }
        if (StringUtils.hasText(databaseId)) {
            builder.setDatabaseId(databaseId);
        }

        return builder.build().getService();
    }
}
