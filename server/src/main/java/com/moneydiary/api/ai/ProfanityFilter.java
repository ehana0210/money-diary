package com.moneydiary.api.ai;

import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 아이용 앱에 부적절한 표현이 포함됐는지 검사하는 간단한 필터.
 *
 * <p>공백/구두점을 제거하고 소문자로 정규화한 뒤 금칙어 포함 여부를 확인한다.
 * 완벽한 필터는 아니며, 명백한 비속어를 1차로 걸러내는 용도다(목록은 확장 가능).
 */
@Component
public class ProfanityFilter {

    private static final Set<String> BAD_WORDS = Set.of(
            "씨발", "시발", "씨바", "ㅅㅂ", "씹", "개새끼", "개세끼", "새끼",
            "병신", "ㅂㅅ", "지랄", "ㅈㄹ", "좆", "좃", "엿먹어", "닥쳐",
            "꺼져", "미친놈", "미친년", "또라이", "개자식", "쌍놈",
            "fuck", "shit", "bitch", "asshole", "dick"
    );

    public boolean isProfane(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = text.toLowerCase().replaceAll("[\\s\\p{Punct}]", "");
        for (String word : BAD_WORDS) {
            if (normalized.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
