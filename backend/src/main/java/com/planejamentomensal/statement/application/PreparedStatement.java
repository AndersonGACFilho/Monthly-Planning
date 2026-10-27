package com.planejamentomensal.statement.application;

import com.planejamentomensal.statement.domain.MemberStatement;

/**
 * A statement ready to export: data, template version and rendering.
 *
 * @param statement statement data
 * @param template  template used
 * @param rendered  HTML and message texts
 * @param email     member e-mail, or {@code null}
 * @param phone     member WhatsApp phone, or {@code null}
 * @since 1.0
 */
public record PreparedStatement(MemberStatement statement, ResolvedTemplate template, RenderedStatement rendered,
        String email, String phone) {
}
