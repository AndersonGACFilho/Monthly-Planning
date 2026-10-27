package com.planejamentomensal.shared.infra.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.planejamentomensal.card.application.SaveCardUseCase;
import com.planejamentomensal.card.domain.Bank;
import com.planejamentomensal.card.domain.CardDetails;
import com.planejamentomensal.entry.application.BillEntryRepository;
import com.planejamentomensal.entry.application.EntryLedger;
import com.planejamentomensal.entry.application.NewPurchase;
import com.planejamentomensal.entry.domain.BillEntry;
import com.planejamentomensal.member.application.SaveMemberUseCase;
import com.planejamentomensal.member.domain.MemberDetails;
import com.planejamentomensal.member.domain.MemberKind;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AuditEventListenerIT {

    @Autowired private SaveMemberUseCase members;
    @Autowired private SaveCardUseCase cards;
    @Autowired private EntryLedger ledger;
    @Autowired private BillEntryRepository entries;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbc;

    @Test
    @DisplayName("RNF-05: changing a confirmed entry records before, after and when")
    void auditsConfirmedEntryChanges() {
        // Given a confirmed entry and a projected one
        long member = members.create(new MemberDetails("AU", "Audit", MemberKind.MAIN, "#000000", null, null, null, "pt-BR")).id();
        long card = cards.create(new CardDetails("Audit", Bank.ITAU, List.of("7777"), "X", null, member), 3, 10).id();
        BillEntry first = ledger.addPurchase(null, new NewPurchase(card, null, false, LocalDate.of(2026, 9, 1), "LOJA 1",
                1, 2, Money.of("10.00"), "PURCHASE", null, null, BillingPeriod.of(2026, 10), LocalDate.of(2026, 9, 1), 1,
                member, null, true));
        entityManager.flush();
        BillEntry projected = entries.findByPurchaseIdOrderByEntryDate(first.purchase().id()).get(1);
        int before = jdbc.queryForObject("select count(*) from audit_entry where entity_type = 'bill-entry'", Integer.class);
        // When both change
        first.adjustAmount(Money.of("12.00"));
        projected.adjustAmount(Money.of("11.00"));
        entityManager.flush();
        // Then only the confirmed entry's change is audited
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select entity_id, before_json, after_json from audit_entry where entity_type = 'bill-entry'");
        assertThat(rows).hasSize(before + 1);
        Map<String, Object> row = rows.getLast();
        assertThat(((Number) row.get("entity_id")).longValue()).isEqualTo(first.id());
        assertThat(row.get("before_json").toString()).contains("10.00");
        assertThat(row.get("after_json").toString()).contains("12.00");
    }
}
