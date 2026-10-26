package com.planejamentomensal.member;

/**
 * Member request bodies shared by tests (TST-05).
 */
public final class MemberTestData {

    private MemberTestData() {
    }

    /**
     * JSON body of a member.
     *
     * @param code code
     * @param name name
     * @param kind MAIN or THIRD_PARTY
     * @return JSON
     */
    public static String memberJson(String code, String name, String kind) {
        return """
                {"code":"%s","name":"%s","kind":"%s","color":"#2563eb","email":"%s@example.com",
                 "whatsappPhone":"+5551999990000","pixKey":"","preferredLanguage":"pt-BR"}
                """.formatted(code, name, kind, code.toLowerCase());
    }
}
