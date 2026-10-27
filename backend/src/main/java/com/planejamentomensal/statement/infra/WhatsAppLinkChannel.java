package com.planejamentomensal.statement.infra;

import com.planejamentomensal.statement.application.DeliveryChannel;
import com.planejamentomensal.statement.application.DeliveryPackage;
import com.planejamentomensal.statement.application.DeliveryResult;
import com.planejamentomensal.statement.application.StatementFiles;
import com.planejamentomensal.statement.domain.DeliveryChannelType;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

/**
 * Prepares a WhatsApp delivery: saves the PNG and returns a {@code wa.me} link with the summary text; the
 * interface opens the link (WhatsApp Desktop or Web, POR-07) and copies the image (RF-87). The user then marks
 * it as sent.
 *
 * @since 1.0
 */
@Component
public class WhatsAppLinkChannel implements DeliveryChannel {

    private final StatementFiles files;

    /**
     * Creates the channel.
     *
     * @param files export folder
     */
    public WhatsAppLinkChannel(StatementFiles files) {
        this.files = files;
    }

    @Override
    public DeliveryChannelType type() {
        return DeliveryChannelType.WHATSAPP;
    }

    @Override
    public DeliveryResult deliver(DeliveryPackage delivery) {
        String digits = delivery.phone() == null ? "" : delivery.phone().replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return DeliveryResult.failed("Member has no WhatsApp number");
        }
        var png = files.saveExport(delivery.statement().billingPeriod(), delivery.rendered().fileBaseName(), "png",
                delivery.png().get());
        return DeliveryResult.awaitingUser(png.toString(), link(digits, delivery.rendered().whatsappText()));
    }

    /**
     * Builds the {@code wa.me} link.
     *
     * @param digits phone digits with country code
     * @param text   message
     * @return link
     */
    static String link(String digits, String text) {
        return "https://wa.me/" + digits + "?text=" + URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
