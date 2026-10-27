package com.planejamentomensal.statement.infra;

import com.planejamentomensal.shared.application.SettingRepository;
import com.planejamentomensal.shared.domain.AppSetting;
import com.planejamentomensal.shared.application.DataFolders;
import com.planejamentomensal.statement.application.SmtpPasswords;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Encrypts the SMTP password with AES-GCM and a random key kept in {@code data/secret.key} (Q9). The app has no
 * login (RNF-10), so this only keeps the password out of plain sight in the database file; the settings screen
 * says so.
 *
 * @since 1.0
 */
@Component
public class AesGcmSmtpPasswords implements SmtpPasswords {

    /** Settings key of the encrypted password; not one of the visible settings. */
    static final String KEY = "smtp-password";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SettingRepository settings;
    private final DataFolders paths;
    private final SecureRandom random = new SecureRandom();

    /**
     * Creates the store.
     *
     * @param settings settings storage
     * @param paths    portable folders
     */
    public AesGcmSmtpPasswords(SettingRepository settings, DataFolders paths) {
        this.settings = settings;
        this.paths = paths;
    }

    @Override
    @Transactional
    public void store(String password) {
        String value = password == null || password.isBlank() ? "" : encrypt(password);
        AppSetting setting = settings.findById(KEY).orElseGet(() -> new AppSetting(KEY, value));
        setting.change(value);
        settings.save(setting);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> read() {
        return settings.findById(KEY).map(AppSetting::value).filter(v -> !v.isEmpty()).map(this::decrypt);
    }

    private String encrypt(String text) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot encrypt the SMTP password", e);
        }
    }

    private String decrypt(String value) {
        try {
            byte[] all = Base64.getDecoder().decode(value);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, all, 0, IV_LENGTH));
            return new String(cipher.doFinal(all, IV_LENGTH, all.length - IV_LENGTH), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Cannot decrypt the SMTP password; store it again in Settings", e);
        }
    }

    private SecretKeySpec key() {
        Path file = paths.data().resolve("secret.key");
        try {
            if (Files.notExists(file)) {
                byte[] key = new byte[32];
                random.nextBytes(key);
                Files.createDirectories(file.getParent());
                Files.write(file, key);
            }
            return new SecretKeySpec(Files.readAllBytes(file), "AES");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
