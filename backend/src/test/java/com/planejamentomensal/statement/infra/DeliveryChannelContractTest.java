package com.planejamentomensal.statement.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.statement.application.DeliveryChannel;
import com.planejamentomensal.statement.application.DeliveryPackage;
import com.planejamentomensal.statement.application.DeliveryResult;
import com.planejamentomensal.statement.application.RenderedStatement;
import com.planejamentomensal.statement.application.SampleStatement;
import com.planejamentomensal.statement.application.StatementFiles;
import com.planejamentomensal.statement.domain.DeliveryChannelType;
import com.planejamentomensal.statement.domain.DeliveryStatus;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DeliveryChannelContractTest {

    @TempDir
    Path dir;

    private final StatementFiles files = new StatementFiles() {
        @Override
        public Path fonts() {
            return dir;
        }

        @Override
        public Path workbookModel() {
            return dir.resolve("model.xlsx");
        }

        @Override
        public void replaceWorkbookModel(byte[] content) {
            // not used
        }

        @Override
        public Path saveExport(BillingPeriod period, String baseName, String extension, byte[] content) {
            return dir.resolve(baseName + "." + extension);
        }
    };

    private DeliveryPackage delivery(String phone) {
        return new DeliveryPackage(SampleStatement.of(BillingPeriod.of(2026, 11)),
                new RenderedStatement("<p/>", "s", "t", "Olá & até já", "pt-BR", false, "extrato-m-2026-11"), null, phone,
                new byte[] {1}, () -> new byte[] {2});
    }

    @Test
    @DisplayName("SOL-03: every channel answers SENT, AWAITING_USER or FAILED and never throws for delivery problems")
    void contract() {
        List<DeliveryChannel> channels = List.of(new WhatsAppLinkChannel(files), new ClipboardChannel(files), new FolderChannel(files));
        Set<DeliveryChannelType> types = EnumSet.noneOf(DeliveryChannelType.class);
        for (DeliveryChannel channel : channels) {
            DeliveryResult result = channel.deliver(delivery("+1 555 0100"));
            assertThat(result.status()).isIn(DeliveryStatus.SENT, DeliveryStatus.AWAITING_USER);
            types.add(channel.type());
        }
        assertThat(types).containsExactlyInAnyOrder(DeliveryChannelType.WHATSAPP, DeliveryChannelType.CLIPBOARD, DeliveryChannelType.FOLDER);
        assertThat(new WhatsAppLinkChannel(files).deliver(delivery(null)).status()).isEqualTo(DeliveryStatus.FAILED);
    }

    @Test
    @DisplayName("RF-87, POR-07: the wa.me link keeps only digits and encodes the message")
    void whatsappLink() {
        DeliveryResult result = new WhatsAppLinkChannel(files).deliver(delivery("+55 (51) 9 9999-0000"));
        assertThat(result.action()).isEqualTo("https://wa.me/5551999990000?text=Ol%C3%A1%20%26%20at%C3%A9%20j%C3%A1");
        assertThat(result.detail()).endsWith("extrato-m-2026-11.png");
        assertThat(new FolderChannel(files).deliver(delivery(null)).detail()).endsWith(".pdf");
    }
}
