package com.moneydiary.api.web;

import com.moneydiary.api.ai.AiException;
import com.moneydiary.api.ai.GeminiService;
import com.moneydiary.api.ai.ProfanityFilter;
import com.moneydiary.api.domain.Category;
import com.moneydiary.api.domain.VoiceLog;
import com.moneydiary.api.repository.CategoryRepository;
import com.moneydiary.api.repository.VoiceLogRepository;
import com.moneydiary.api.security.FirebaseUser;
import com.moneydiary.api.web.dto.ParseTransactionRequest;
import com.moneydiary.api.web.dto.ParsedTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * LLM 기반 기능 엔드포인트.
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final GeminiService geminiService;
    private final CategoryRepository categoryRepository;
    private final VoiceLogRepository voiceLogRepository;
    private final ProfanityFilter profanityFilter;

    public AiController(GeminiService geminiService,
                        CategoryRepository categoryRepository,
                        VoiceLogRepository voiceLogRepository,
                        ProfanityFilter profanityFilter) {
        this.geminiService = geminiService;
        this.categoryRepository = categoryRepository;
        this.voiceLogRepository = voiceLogRepository;
        this.profanityFilter = profanityFilter;
    }

    /**
     * 자연어/음성 한 줄을 거래 후보로 변환한다. 저장은 하지 않고 해석 결과만 반환하며,
     * 모든 요청(유효/필터/오류)을 voiceLogs 에 기록한다.
     */
    @PostMapping("/parse-transaction")
    public ParsedTransaction parseTransaction(@AuthenticationPrincipal FirebaseUser user,
                                              @RequestBody ParseTransactionRequest request) {
        if (request == null || request.text() == null || request.text().isBlank()) {
            throw new IllegalArgumentException("text 는 필수입니다.");
        }
        String uid = user.uid();
        String text = request.text().trim();
        String source = (request.source() == null || request.source().isBlank())
                ? "voice" : request.source();

        // 1) 비속어/부적절 표현 필터 (LLM 호출 전에 차단)
        if (profanityFilter.isProfane(text)) {
            ParsedTransaction result = ParsedTransaction.invalid("바르고 고운 말로 다시 말해줄래요?");
            saveLog(uid, text, source, VoiceLog.STATUS_FILTERED_PROFANITY, result);
            return result;
        }

        // 2) LLM 파싱
        ParsedTransaction parsed;
        try {
            List<Category> categories = categoryRepository.findAll(uid);
            parsed = geminiService.parseTransaction(text, categories, LocalDate.now(SEOUL));
        } catch (AiException e) {
            saveLog(uid, text, source, VoiceLog.STATUS_ERROR, ParsedTransaction.invalid(e.getMessage()));
            throw e;
        }

        // 3) 유효성에 따라 상태 기록
        String status = parsed.valid()
                ? VoiceLog.STATUS_ACCEPTED
                : VoiceLog.STATUS_FILTERED_NON_TX;
        saveLog(uid, text, source, status, parsed);
        return parsed;
    }

    /**
     * 최근 음성/자연어 입력 기록 조회(분석용). 기본 50건, 최대 200건.
     */
    @GetMapping("/voice-logs")
    public List<VoiceLog> voiceLogs(@AuthenticationPrincipal FirebaseUser user,
                                    @RequestParam(name = "limit", defaultValue = "50") int limit) {
        int capped = Math.max(1, Math.min(limit, 200));
        return voiceLogRepository.findRecent(user.uid(), capped);
    }

    private void saveLog(String uid, String text, String source, String status, ParsedTransaction parsed) {
        try {
            VoiceLog logEntry = new VoiceLog();
            logEntry.setRawText(text);
            logEntry.setSource(source);
            logEntry.setStatus(status);
            logEntry.setValid(parsed.valid());
            logEntry.setReason(parsed.reason() == null ? "" : parsed.reason());
            logEntry.setType(parsed.type() == null ? null : parsed.type().name());
            logEntry.setAmount(parsed.amount());
            logEntry.setCategoryId(parsed.category());
            logEntry.setCategoryName(parsed.categoryName());
            logEntry.setDate(parsed.date());
            logEntry.setMemo(parsed.memo() == null ? "" : parsed.memo());
            logEntry.setCreatedAt(System.currentTimeMillis());
            voiceLogRepository.create(uid, logEntry);
        } catch (RuntimeException e) {
            // 로깅 실패가 사용자 응답을 막지 않도록 한다.
            log.warn("voiceLog 적재 실패 (uid={}, status={})", uid, status, e);
        }
    }
}
