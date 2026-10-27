package com.planejamentomensal.statement.infra;

import com.planejamentomensal.statement.application.DeliveryChannel;
import com.planejamentomensal.statement.application.DeliveryPackage;
import com.planejamentomensal.statement.application.DeliveryResult;
import com.planejamentomensal.statement.application.StatementFiles;
import com.planejamentomensal.statement.domain.DeliveryChannelType;
import org.springframework.stereotype.Component;

/**
 * Saves the statement PNG; the interface copies it to the clipboard for pasting in any app (RF-88).
 *
 * @since 1.0
 */
@Component
public class ClipboardChannel implements DeliveryChannel {

    private final StatementFiles files;

    /**
     * Creates the channel.
     *
     * @param files export folder
     */
    public ClipboardChannel(StatementFiles files) {
        this.files = files;
    }

    @Override
    public DeliveryChannelType type() {
        return DeliveryChannelType.CLIPBOARD;
    }

    @Override
    public DeliveryResult deliver(DeliveryPackage delivery) {
        var png = files.saveExport(delivery.statement().billingPeriod(), delivery.rendered().fileBaseName(), "png",
                delivery.png().get());
        return DeliveryResult.awaitingUser(png.toString(), null);
    }
}
