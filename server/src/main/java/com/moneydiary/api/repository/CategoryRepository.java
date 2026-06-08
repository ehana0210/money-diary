package com.moneydiary.api.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.moneydiary.api.domain.Category;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * users/{uid}/categories 서브컬렉션에 대한 Firestore 접근.
 */
@Repository
public class CategoryRepository {

    private final Firestore firestore;

    public CategoryRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    private CollectionReference collection(String uid) {
        return firestore.collection("users").document(uid).collection("categories");
    }

    public Category create(String uid, Category category) {
        DocumentReference ref = collection(uid).document();
        await(ref.set(category));
        category.setId(ref.getId());
        return category;
    }

    public List<Category> findAll(String uid) {
        List<QueryDocumentSnapshot> docs = await(
                collection(uid).orderBy("createdAt", Query.Direction.ASCENDING).get())
                .getDocuments();

        List<Category> result = new ArrayList<>(docs.size());
        for (QueryDocumentSnapshot doc : docs) {
            result.add(toEntity(doc));
        }
        return result;
    }

    public Category update(String uid, String id, Category category) {
        DocumentReference ref = collection(uid).document(id);
        if (!await(ref.get()).exists()) {
            throw new ResourceNotFoundException("카테고리를 찾을 수 없습니다: " + id);
        }
        await(ref.set(category));
        category.setId(id);
        return category;
    }

    public void delete(String uid, String id) {
        DocumentReference ref = collection(uid).document(id);
        if (!await(ref.get()).exists()) {
            throw new ResourceNotFoundException("카테고리를 찾을 수 없습니다: " + id);
        }
        await(ref.delete());
    }

    private Category toEntity(DocumentSnapshot doc) {
        Category category = doc.toObject(Category.class);
        if (category != null) {
            category.setId(doc.getId());
        }
        return category;
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
