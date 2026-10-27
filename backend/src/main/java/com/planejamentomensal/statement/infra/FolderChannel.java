package com.planejamentomensal.statement.infra;

import com.planejamentomensal.statement.application.DeliveryChannel;
import com.planejamentomensal.statement.application.DeliveryPackage;
import com.planejamentomensal.statement.application.DeliveryResult;
import com.planejamentomensal.statement.application.StatementFiles;
import com.planejamentomensal.statement.domain.DeliveryChannelType;
import org.springframework.stereotype.Component;

/**
 * Saves the statement PDF in {@code data/exports/<billing-period>/}; the interface can open the folder (RF-89,
 * POR-04).
 *
 * @since 1.0
 */
@Component
public class FolderChannel implements DeliveryChannel {

    private final StatementFiles files;

    /**
     * Creates the channel.
     *
     * @param files export folder
     */
    public FolderChannel(StatementFiles files) {
        this.files = files;
    }

    @Override
    public DeliveryChannelType type() {
        return DeliveryChannelType.FOLDER;
    }

    @Override
    public DeliveryResult deliver(DeliveryPackage delivery) {
        var pdf = files.saveExport(delivery.statement().billingPeriod(), delivery.rendered().fileBaseName(), "pdf",
                delivery.pdf());
        return DeliveryResult.sent(pdf.toString());
    }
}
