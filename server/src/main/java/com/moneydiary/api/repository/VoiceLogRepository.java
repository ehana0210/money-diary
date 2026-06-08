package com.moneydiary.api.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.moneydiary.api.domain.VoiceLog;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * users/{uid}/voiceLogs 서브컬렉션에 대한 Firestore 접근.
 */
@Repository
public class VoiceLogRepository {

    private final Firestore firestore;

    public VoiceLogRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    private CollectionReference collection(String uid) {
        return firestore.collection("users").document(uid).collection("voiceLogs");
    }

    public VoiceLog create(String uid, VoiceLog log) {
        DocumentReference ref = collection(uid).document();
        await(ref.set(log));
        log.setId(ref.getId());
        return log;
    }

    /** 최근 기록부터 최대 {@code limit} 건 조회. */
    public List<VoiceLog> findRecent(String uid, int limit) {
        List<QueryDocumentSnapshot> docs = await(
                collection(uid)
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .limit(limit)
                        .get())
                .getDocuments();

        List<VoiceLog> result = new ArrayList<>(docs.size());
        for (QueryDocumentSnapshot doc : docs) {
            result.add(toEntity(doc));
        }
        return result;
    }

    private VoiceLog toEntity(DocumentSnapshot doc) {
        VoiceLog log = doc.toObject(VoiceLog.class);
        if (log != null) {
            log.setId(doc.getId());
        }
        return log;
    }

    private static <T> T await(ApiFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Firestore 작업이 중단되었습니다.", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Firestore 작업에 실패했습니다.", e.getCause());
        }
    }
}
