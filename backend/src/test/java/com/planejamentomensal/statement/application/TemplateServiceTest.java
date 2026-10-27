package com.planejamentomensal.statement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.planejamentomensal.member.application.SaveMemberUseCase;
import com.planejamentomensal.member.domain.MemberDetails;
import com.planejamentomensal.member.domain.MemberKind;
import com.planejamentomensal.shared.domain.BusinessRuleException;
import com.planejamentomensal.statement.domain.Template;
import com.planejamentomensal.statement.domain.TemplateFont;
import com.planejamentomensal.statement.domain.TemplateSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class TemplateServiceTest {

    @Autowired private TemplateService templates;
    @Autowired private SaveMemberUseCase members;

    @Test
    @DisplayName("TPL-04, TPL-06: default template on first use, new versions on change, one default at a time")
    void versionsAndDefault() {
        Template standard = templates.defaultTemplate();
        assertThat(standard.isDefault()).isTrue();
        assertThat(templates.settings(standard).texts()).containsKeys("pt-BR", "en-US");
        TemplateSettings base = templates.defaultSettings();
        Template dark = templates.create("Escuro", base);
        assertThat(dark.isDefault()).isFalse();
        TemplateSettings serif = new TemplateSettings("#111111", base.secondaryColor(), TemplateFont.NOTO_SERIF, null,
                base.blocks(), base.texts(), "h1 { letter-spacing: 1px; }", null);
        Template changed = templates.update(dark.id(), "Escuro", serif, dark.version());
        assertThat(changed.currentVersion()).isEqualTo(2);
        assertThat(templates.versions(dark.id())).extracting(v -> v.number()).containsExactly(2, 1);
        templates.update(dark.id(), "Escuro 2", serif, null);
        assertThat(templates.versions(dark.id())).hasSize(2);
        templates.makeDefault(dark.id());
        assertThat(templates.get(standard.id()).isDefault()).isFalse();
        assertThatThrownBy(() -> templates.delete(dark.id())).isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", "template-default-delete");
        templates.delete(standard.id());
        assertThat(templates.all()).extracting(Template::id).containsExactly(dark.id());
    }

    @Test
    @DisplayName("TPL-04: a member can use its own template; clearing goes back to the default")
    void perMember() {
        long member = members.create(new MemberDetails("TT", "Tia", MemberKind.THIRD_PARTY, "#000000", null, null, null, "en-US")).id();
        Template standard = templates.defaultTemplate();
        Template own = templates.create("Tia", templates.defaultSettings());
        templates.assign(member, own.id());
        assertThat(templates.resolve(member, null).templateId()).isEqualTo(own.id());
        assertThat(templates.assignments()).containsEntry(member, own.id());
        assertThat(templates.resolve(member, standard.id()).templateId()).isEqualTo(standard.id());
        templates.assign(member, null);
        assertThat(templates.resolve(member, null).templateId()).isEqualTo(standard.id());
        templates.assign(member, own.id());
        templates.delete(own.id());
        assertThat(templates.resolve(member, null).templateId()).isEqualTo(standard.id());
    }
}
