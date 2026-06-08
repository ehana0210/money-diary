package com.moneydiary.api.repository;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.moneydiary.api.domain.Transaction;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * users/{uid}/transactions 서브컬렉션에 대한 Firestore 접근.
 *
 * <p>Spring MVC 는 블로킹 모델이므로 {@code ApiFuture#get()} 으로 동기 처리한다.
 */
@Repository
public class TransactionRepository {

    private final Firestore firestore;

    public TransactionRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    private CollectionReference collection(String uid) {
        return firestore.collection("users").document(uid).collection("transactions");
    }

    public Transaction create(String uid, Transaction transaction) {
        DocumentReference ref = collection(uid).document();
        await(ref.set(transaction));
        transaction.setId(ref.getId());
        return transaction;
    }

    public List<Transaction> findAll(String uid) {
        List<QueryDocumentSnapshot> docs = await(
                collection(uid).orderBy("date", Query.Direction.DESCENDING)
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .get())
                .getDocuments();

        List<Transaction> result = new ArrayList<>(docs.size());
        for (QueryDocumentSnapshot doc : docs) {
            result.add(toEntity(doc));
        }
        return result;
    }

    public Transaction findById(String uid, String id) {
        DocumentSnapshot snapshot = await(collection(uid).document(id).get());
        if (!snapshot.exists()) {
            throw new ResourceNotFoundException("거래를 찾을 수 없습니다: " + id);
        }
        return toEntity(snapshot);
    }

    public Transaction update(String uid, String id, Transaction transaction) {
        DocumentReference ref = collection(uid).document(id);
        if (!await(ref.get()).exists()) {
            throw new ResourceNotFoundException("거래를 찾을 수 없습니다: " + id);
        }
        await(ref.set(transaction));
        transaction.setId(id);
        return transaction;
    }

    public void delete(String uid, String id) {
        DocumentReference ref = collection(uid).document(id);
        if (!await(ref.get()).exists()) {
            throw new ResourceNotFoundException("거래를 찾을 수 없습니다: " + id);
        }
        await(ref.delete());
    }

    private Transaction toEntity(DocumentSnapshot doc) {
        Transaction transaction = doc.toObject(Transaction.class);
        if (transaction != null) {
            transaction.setId(doc.getId());
        }
        return transaction;
    }

    private static <T> T await(com.google.api.core.ApiFuture<T> future) {
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
