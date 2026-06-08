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
 * 최상위 {@code voiceLogs} 컬렉션에 대한 Firestore 접근.
 *
 * <p>사용자 데이터(users/{uid}/...)와 분리해 별도 컬렉션에 적재하며,
 * 각 문서의 {@code uid} 필드로 사용자를 구분한다.
 */
@Repository
public class VoiceLogRepository {

    private static final String COLLECTION = "voiceLogs";

    private final Firestore firestore;

    public VoiceLogRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    private CollectionReference collection() {
        return firestore.collection(COLLECTION);
    }

    public VoiceLog create(String uid, VoiceLog log) {
        log.setUid(uid);
        DocumentReference ref = collection().document();
        await(ref.set(log));
        log.setId(ref.getId());
        return log;
    }

    /** 해당 사용자의 최근 기록부터 최대 {@code limit} 건 조회. */
    public List<VoiceLog> findRecent(String uid, int limit) {
        List<QueryDocumentSnapshot> docs = await(
                collection()
                        .whereEqualTo("uid", uid)
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
