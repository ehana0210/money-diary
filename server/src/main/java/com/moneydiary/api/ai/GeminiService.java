package com.moneydiary.api.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.moneydiary.api.domain.Category;
import com.moneydiary.api.domain.TransactionType;
import com.moneydiary.api.web.dto.ParsedTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * 자연어 한 줄을 Gemini(Vertex AI)로 거래 후보로 변환한다.
 */
@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private final Client client;
    private final ObjectMapper objectMapper;
    private final String model;

    public GeminiService(Client client,
                         ObjectMapper objectMapper,
                         @Value("${gcp.vertex.model:gemini-2.5-flash}") String model) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.model = model;
    }

    public ParsedTransaction parseTransaction(String text, List<Category> categories, LocalDate today) {
        String prompt = buildPrompt(text, categories, today);

        GenerateContentConfig config = GenerateContentConfig.builder()
                .temperature(0.1F)
                .responseMimeType("application/json")
                .build();

        String json;
        try {
            GenerateContentResponse response = client.models.generateContent(model, prompt, config);
            json = response.text();
        } catch (RuntimeException e) {
            log.error("Gemini 호출 실패", e);
            throw new AiException("AI 분석에 실패했습니다.", e);
        }

        if (json == null || json.isBlank()) {
            throw new AiException("AI 응답이 비어 있습니다.");
        }
        return toParsed(json, categories, today);
    }

    private ParsedTransaction toParsed(String json, List<Category> categories, LocalDate today) {
        JsonNode node;
        try {
            node = objectMapper.readTree(json);
        } catch (Exception e) {
            log.error("AI 응답 JSON 파싱 실패: {}", json, e);
            throw new AiException("AI 응답을 해석할 수 없습니다.");
        }

        TransactionType type = "INCOME".equalsIgnoreCase(text(node, "type"))
                ? TransactionType.INCOME
                : TransactionType.EXPENSE;

        long amount = node.path("amount").asLong(0);

        String date = text(node, "date");
        if (!isValidDate(date)) {
            date = today.format(DateTimeFormatter.ISO_LOCAL_DATE);
        }

        String memo = text(node, "memo");
        if (memo == null) {
            memo = "";
        }

        String categoryName = text(node, "category");
        String matchedId = null;
        if (categoryName != null && !categoryName.isBlank()) {
            for (Category c : categories) {
                boolean sameType = c.getType() == null || c.getType() == type;
                if (sameType && categoryName.trim().equalsIgnoreCase(c.getName())) {
                    matchedId = c.getId();
                    categoryName = c.getName();
                    break;
                }
            }
        }

        // 유효성: LLM 의 valid 판단 + 금액(>0) 확인
        boolean llmValid = node.path("valid").asBoolean(false);
        String reason = text(node, "reason");
        if (reason == null) {
            reason = "";
        }

        if (!llmValid) {
            if (reason.isBlank()) {
                reason = "거래 내용으로 이해하지 못했어요.";
            }
            return ParsedTransaction.invalid(reason);
        }
        if (amount <= 0) {
            return ParsedTransaction.invalid("금액을 알아듣지 못했어요.");
        }

        return new ParsedTransaction(true, "", type, amount, matchedId, categoryName,
                matchedId != null, date, memo);
    }

    private String buildPrompt(String text, List<Category> categories, LocalDate today) {
        String expense = names(categories, TransactionType.EXPENSE);
        String income = names(categories, TransactionType.INCOME);

        return new StringJoiner("\n")
                .add("너는 초등학생용 용돈기입장 앱의 입력 도우미다.")
                .add("사용자가 자연어로 쓴 한 줄을 거래 1건으로 변환하라.")
                .add("오늘 날짜는 " + today.format(DateTimeFormatter.ISO_LOCAL_DATE) + " (Asia/Seoul) 이다.")
                .add("")
                .add("먼저 입력이 '돈을 쓰거나 받은 거래'인지 판단하라:")
                .add("- 거래가 아니면(인사, 잡담, 질문, 무의미한 말, 금액을 알 수 없는 말 등) valid=false 로 하고 reason 에 한 문장으로 이유를 써라.")
                .add("- 거래면 valid=true 로 하고 아래 규칙대로 채워라.")
                .add("")
                .add("규칙:")
                .add("- type: 돈을 쓴 것이면 \"EXPENSE\", 받은 것이면 \"INCOME\".")
                .add("- amount: 원 단위 정수(숫자만, 쉼표/단위 제외). 알 수 없으면 valid=false.")
                .add("- date: yyyy-MM-dd 형식. \"어제\",\"오늘\",\"그저께\",\"지난주 금요일\" 같은 상대 표현은 오늘 기준으로 계산하라.")
                .add("- category: 아래 목록 중 가장 알맞은 이름 하나를 그대로 사용. 적당한 게 없으면 \"기타\".")
                .add("- memo: 핵심만 짧게. 없으면 빈 문자열.")
                .add("")
                .add("지출 카테고리: " + expense)
                .add("수입 카테고리: " + income)
                .add("")
                .add("반드시 아래 JSON 형식으로만, 다른 설명 없이 답하라:")
                .add("{\"valid\":true,\"reason\":\"\",\"type\":\"EXPENSE\",\"amount\":3000,\"category\":\"간식\",\"date\":\""
                        + today.format(DateTimeFormatter.ISO_LOCAL_DATE) + "\",\"memo\":\"떡볶이\"}")
                .add("거래가 아니면 예: {\"valid\":false,\"reason\":\"거래 내용이 아니에요.\",\"type\":\"EXPENSE\",\"amount\":0,\"category\":\"기타\",\"date\":\""
                        + today.format(DateTimeFormatter.ISO_LOCAL_DATE) + "\",\"memo\":\"\"}")
                .add("")
                .add("사용자 입력: \"" + (text == null ? "" : text.replace("\"", "'")) + "\"")
                .toString();
    }

    private static String names(List<Category> categories, TransactionType type) {
        String joined = categories.stream()
                .filter(c -> c.getType() == null || c.getType() == type)
                .map(Category::getName)
                .filter(n -> n != null && !n.isBlank())
                .collect(Collectors.joining(", "));
        return joined.isBlank() ? "(없음)" : joined;
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    private static boolean isValidDate(String s) {
        if (s == null || s.isBlank()) {
            return false;
        }
        try {
            LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
