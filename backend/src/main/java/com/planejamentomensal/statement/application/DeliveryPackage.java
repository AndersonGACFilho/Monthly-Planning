package com.planejamentomensal.statement.application;

import com.planejamentomensal.statement.domain.MemberStatement;
import java.util.function.Supplier;

/**
 * Everything a channel needs to deliver one statement.
 *
 * @param statement statement data
 * @param rendered  HTML and message texts in the delivery language
 * @param email     member e-mail, or {@code null}
 * @param phone     member WhatsApp phone with country code, or {@code null}
 * @param pdf       PDF bytes
 * @param png       PNG bytes, made on demand
 * @since 1.0
 */
public record DeliveryPackage(MemberStatement statement, RenderedStatement rendered, String email, String phone,
        byte[] pdf, Supplier<byte[]> png) {
}
