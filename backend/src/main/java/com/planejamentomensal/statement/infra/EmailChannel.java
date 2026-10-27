package com.planejamentomensal.statement.infra;

import com.planejamentomensal.shared.application.Setting;
import com.planejamentomensal.shared.application.Settings;
import com.planejamentomensal.statement.application.DeliveryChannel;
import com.planejamentomensal.statement.application.DeliveryPackage;
import com.planejamentomensal.statement.application.DeliveryResult;
import com.planejamentomensal.statement.application.SmtpPasswords;
import com.planejamentomensal.statement.domain.DeliveryChannelType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Sends the statement by e-mail through the SMTP server of the settings: subject and text from the template,
 * the statement HTML as body and the PDF attached (RF-86).
 *
 * @since 1.0
 */
@Component
public class EmailChannel implements DeliveryChannel {

    private static final int TIMEOUT_MILLIS = 20_000;

    private final Settings settings;
    private final SmtpPasswords passwords;

    /**
     * Creates the channel.
     *
     * @param settings  SMTP settings
     * @param passwords SMTP password
     */
    public EmailChannel(Settings settings, SmtpPasswords passwords) {
        this.settings = settings;
        this.passwords = passwords;
    }

    @Override
    public DeliveryChannelType type() {
        return DeliveryChannelType.EMAIL;
    }

    @Override
    public DeliveryResult deliver(DeliveryPackage delivery) {
        String host = settings.get(Setting.SMTP_HOST);
        if (host.isBlank()) {
            return DeliveryResult.failed("SMTP server not configured");
        }
        if (delivery.email() == null || delivery.email().isBlank()) {
            return DeliveryResult.failed("Member has no e-mail address");
        }
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(settings.getInt(Setting.SMTP_PORT));
        String user = settings.get(Setting.SMTP_USERNAME);
        if (!user.isBlank()) {
            sender.setUsername(user);
            passwords.read().ifPresent(sender::setPassword);
        }
        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", String.valueOf(!user.isBlank()));
        props.put("mail.smtp.starttls.enable", String.valueOf(settings.getBoolean(Setting.SMTP_STARTTLS)));
        props.put("mail.smtp.connectiontimeout", String.valueOf(TIMEOUT_MILLIS));
        props.put("mail.smtp.timeout", String.valueOf(TIMEOUT_MILLIS));
        props.put("mail.smtp.writetimeout", String.valueOf(TIMEOUT_MILLIS));
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            String from = settings.get(Setting.SMTP_FROM);
            helper.setFrom(from.isBlank() ? user : from);
            helper.setTo(delivery.email());
            helper.setSubject(delivery.rendered().subject());
            helper.setText(delivery.rendered().emailText(), delivery.rendered().html());
            helper.addAttachment(delivery.rendered().fileBaseName() + ".pdf", new ByteArrayResource(delivery.pdf()),
                    "application/pdf");
            sender.send(message);
            return DeliveryResult.sent(delivery.email());
        } catch (MessagingException | MailException e) {
            return DeliveryResult.failed(e.getMessage());
        }
    }
}
