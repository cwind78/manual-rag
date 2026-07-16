package com.prj.manualrag.chunk;

import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

@Component
public class HeadingDetector {
    /*
     * 숫자 목차
     *
     * 예:
     * 1.
     * 1.1
     * 3.2.1
     */
    private static final Pattern NUMBER_PATTERN =
            Pattern.compile(
                    "^\\d+(\\.\\d+)*\\.?\\s+.*"
            );


    /*
     * Chapter, Section 형태
     */
    private static final Pattern SECTION_PATTERN =
            Pattern.compile(
                    "^(chapter|section|part)\\s+.*",
                    Pattern.CASE_INSENSITIVE
            );


    /*
     * 한글 장/절
     *
     * 예:
     * 제1장 개요
     * 제2절 설치 방법
     */
    private static final Pattern KOREAN_PATTERN =
            Pattern.compile(
                    "^제\\s*\\d+\\s*(장|절|편).*"
            );


    /**
     * 제목 여부 판단
     */
    public boolean isHeading(
            String text
    ) {

        if (text == null || text.isBlank()) {
            return false;
        }


        String value =
                text.trim();


        return NUMBER_PATTERN.matcher(value).matches()
                || SECTION_PATTERN.matcher(value).matches()
                || KOREAN_PATTERN.matcher(value).matches()
                || isShortUpperCaseTitle(value);
    }



    /**
     * 영어 문서에서 흔한 제목 형태
     *
     * 예:
     * CONFIGURATION
     * INSTALLATION GUIDE
     */
    private boolean isShortUpperCaseTitle(
            String text
    ) {

        if (text.length() > 80) {
            return false;
        }


        return text.equals(
                text.toUpperCase()
        );
    }

}
