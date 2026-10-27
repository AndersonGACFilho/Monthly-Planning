package com.planejamentomensal.statement.infra;

import static com.planejamentomensal.statement.application.StatementFixture.NOV;
import static org.assertj.core.api.Assertions.assertThat;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.planejamentomensal.member.application.SaveMemberUseCase;
import com.planejamentomensal.member.domain.MemberDetails;
import com.planejamentomensal.member.domain.MemberKind;
import com.planejamentomensal.shared.application.Settings;
import com.planejamentomensal.statement.application.DeliveryOutcome;
import com.planejamentomensal.statement.application.SendRequest;
import com.planejamentomensal.statement.application.SendStatementUseCase;
import com.planejamentomensal.statement.application.SmtpPasswords;
import com.planejamentomensal.statement.domain.DeliveryChannelType;
import com.planejamentomensal.statement.domain.DeliveryStatus;
import com.planejamentomensal.statement.domain.StatementOptions;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class EmailChannelIT {

    @RegisterExtension
    static final GreenMailExtension MAIL = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(com.icegreen.greenmail.configuration.GreenMailConfiguration.aConfig()
                    .withUser("app@example.com", "app@example.com", "s3cret"));

    @Autowired private SaveMemberUseCase members;
    @Autowired private Settings settings;
    @Autowired private SmtpPasswords passwords;
    @Autowired private SendStatementUseCase send;

    @Test
    @DisplayName("RF-86, I18N-08: e-mail via SMTP with template subject, HTML body and the PDF attached, in the member language")
    void sendsEmail() throws Exception {
        settings.change(Map.of("smtp-host", "127.0.0.1", "smtp-port", String.valueOf(ServerSetupTest.SMTP.getPort()),
                "smtp-username", "app@example.com", "smtp-from", "app@example.com", "smtp-starttls", "false"));
        passwords.store("s3cret");
        assertThat(passwords.read()).contains("s3cret");
        long member = members.create(new MemberDetails("E1", "Eva", MemberKind.THIRD_PARTY, "#000000", "eva@example.com", null,
                null, "en-US")).id();
        DeliveryOutcome outcome = send.send(new SendRequest(NOV, List.of(member), DeliveryChannelType.EMAIL,
                StatementOptions.DEFAULT, null, null, null, true)).getFirst();
        assertThat(outcome.status()).as(outcome.detail()).isEqualTo(DeliveryStatus.SENT);
        MimeMessage[] received = MAIL.getReceivedMessages();
        assertThat(received).hasSize(1);
        assertThat(received[0].getSubject()).isEqualTo("Statement November 2026: R$0.00");
        assertThat(received[0].getAllRecipients()[0].toString()).isEqualTo("eva@example.com");
        String raw = GreenMailUtil.getWholeMessage(received[0]);
        assertThat(raw).contains("application/pdf").contains("statement-e1-2026-11.pdf");
        assertThat(((MimeMultipart) received[0].getContent()).getCount()).isGreaterThanOrEqualTo(2);
        passwords.store("");
        assertThat(passwords.read()).isEmpty();
    }
}
