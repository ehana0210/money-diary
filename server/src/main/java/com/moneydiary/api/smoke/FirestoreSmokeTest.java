package com.moneydiary.api.smoke;

import com.moneydiary.api.domain.Category;
import com.moneydiary.api.domain.Transaction;
import com.moneydiary.api.domain.TransactionType;
import com.moneydiary.api.repository.CategoryRepository;
import com.moneydiary.api.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 실제 Firestore 연결을 검증하는 일회성 스모크 테스트.
 *
 * <p>인증(Firebase ID 토큰) 없이 리포지토리를 통해 임시 사용자(uid={@code smoketest-user})
 * 컬렉션에 거래/카테고리를 write → read → delete 한 뒤 애플리케이션을 종료한다.
 *
 * <p>{@code smoketest} 프로파일에서만 동작하므로 평소 실행에는 영향이 없다:
 * <pre>
 *   GCP_PROJECT_ID=your-project-id \
 *   ./gradlew bootRun --args='--spring.profiles.active=smoketest'
 * </pre>
 */
@Component
@Profile("smoketest")
public class FirestoreSmokeTest implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(FirestoreSmokeTest.class);
    private static final String TEST_UID = "smoketest-user";

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final ApplicationContext context;

    public FirestoreSmokeTest(TransactionRepository transactionRepository,
                              CategoryRepository categoryRepository,
                              ApplicationContext context) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.context = context;
    }

    @Override
    public void run(String... args) {
        int exitCode = 0;
        try {
            log.info("[SMOKE] Firestore 연결 검증 시작 (uid={})", TEST_UID);

            Category category = new Category();
            category.setName("간식");
            category.setType(TransactionType.EXPENSE);
            category.setCreatedAt(System.currentTimeMillis());
            Category savedCategory = categoryRepository.create(TEST_UID, category);
            log.info("[SMOKE] 카테고리 생성 OK id={}", savedCategory.getId());

            Transaction tx = new Transaction();
            tx.setType(TransactionType.EXPENSE);
            tx.setAmount(1500);
            tx.setCategory("간식");
            tx.setDate("2026-06-08");
            tx.setMemo("스모크 테스트");
            tx.setCreatedAt(System.currentTimeMillis());
            Transaction savedTx = transactionRepository.create(TEST_UID, tx);
            log.info("[SMOKE] 거래 생성 OK id={}", savedTx.getId());

            Transaction fetched = transactionRepository.findById(TEST_UID, savedTx.getId());
            log.info("[SMOKE] 거래 조회 OK amount={} category={} date={}",
                    fetched.getAmount(), fetched.getCategory(), fetched.getDate());

            List<Transaction> all = transactionRepository.findAll(TEST_UID);
            log.info("[SMOKE] 거래 목록 조회 OK count={}", all.size());

            // 테스트 사용자(smoketest-user)의 모든 잔여 문서까지 정리해 멱등성을 보장한다.
            int removedTx = 0;
            for (Transaction t : transactionRepository.findAll(TEST_UID)) {
                transactionRepository.delete(TEST_UID, t.getId());
                removedTx++;
            }
            int removedCat = 0;
            for (Category c : categoryRepository.findAll(TEST_UID)) {
                categoryRepository.delete(TEST_UID, c.getId());
                removedCat++;
            }
            log.info("[SMOKE] 정리(delete) OK transactions={} categories={}", removedTx, removedCat);

            log.info("[SMOKE] ✅ Firestore 연결/저장/조회/삭제 모두 성공");
        } catch (Exception e) {
            exitCode = 1;
            log.error("[SMOKE] ❌ Firestore 검증 실패: {}", e.getMessage(), e);
        } finally {
            final int code = exitCode;
            System.exit(SpringApplication.exit(context, () -> code));
        }
    }
}
