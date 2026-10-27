package com.planejamentomensal.recurrence.application;

import com.planejamentomensal.member.application.MemberQueries;
import com.planejamentomensal.recurrence.domain.Occurrence;
import com.planejamentomensal.recurrence.domain.OccurrenceStatus;
import com.planejamentomensal.recurrence.domain.RecurrenceRule;
import com.planejamentomensal.recurrence.domain.RuleDetails;
import com.planejamentomensal.recurrence.domain.RuleKind;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.BusinessRuleException;
import com.planejamentomensal.shared.domain.Money;
import com.planejamentomensal.shared.domain.NotFoundException;
import com.planejamentomensal.shared.domain.Versions;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates and edits income and fixed expense rules and their monthly occurrences (RF-21 to RF-24,
 * RF-30 to RF-33, RN-30).
 *
 * @since 1.0
 */
@Service
@Transactional
public class RecurrenceCommands {

    private final RecurrenceRuleRepository rules;
    private final OccurrenceRepository occurrences;
    private final GenerateOccurrencesUseCase generator;
    private final FixedExpenseLinker linker;
    private final MemberQueries members;

    /**
     * Creates the commands.
     *
     * @param rules       rule storage
     * @param occurrences occurrence storage
     * @param generator   occurrence generator
     * @param linker      card link of fixed expenses
     * @param members     member queries
     */
    public RecurrenceCommands(RecurrenceRuleRepository rules, OccurrenceRepository occurrences,
            GenerateOccurrencesUseCase generator, FixedExpenseLinker linker, MemberQueries members) {
        this.rules = rules;
        this.occurrences = occurrences;
        this.generator = generator;
        this.linker = linker;
        this.members = members;
    }

    /**
     * Returns the occurrences of a period, generated on demand; card-paid expenses are linked (RF-22, RF-31,
     * RF-33).
     *
     * @param period billing period
     * @param kind   income or fixed expense
     * @return occurrences
     */
    public List<Occurrence> occurrences(BillingPeriod period, RuleKind kind) {
        List<Occurrence> list = generator.generate(period, kind);
        if (kind == RuleKind.FIXED_EXPENSE) {
            linker.link(list);
        }
        return list;
    }

    /**
     * Creates a rule.
     *
     * @param kind    income or fixed expense
     * @param details rule data
     * @return the rule
     * @throws BusinessRuleException with code {@code card-member-unknown} for an unknown member
     */
    public RecurrenceRule create(RuleKind kind, RuleDetails details) {
        checkMember(details.memberId());
        return rules.saveAndFlush(new RecurrenceRule(kind, details));
    }

    /**
     * Replaces the data of a rule; generated periods keep the amounts set by hand (RF-22).
     *
     * @param id              rule
     * @param details         new data
     * @param expectedVersion version the client edited, or {@code null}
     * @return the rule
     */
    public RecurrenceRule update(long id, RuleDetails details, Long expectedVersion) {
        RecurrenceRule rule = rule(id);
        Versions.check(expectedVersion, rule.version());
        checkMember(details.memberId());
        rule.update(details);
        return rules.saveAndFlush(rule);
    }

    /**
     * Activates or inactivates a rule.
     *
     * @param id     rule
     * @param active new state
     * @return the rule
     */
    public RecurrenceRule setActive(long id, boolean active) {
        RecurrenceRule rule = rule(id);
        rule.setActive(active);
        return rules.saveAndFlush(rule);
    }

    /**
     * Deletes a rule with all its occurrences.
     *
     * @param id rule
     * @throws BusinessRuleException with code {@code rule-has-history} if a period was received or paid
     */
    public void delete(long id) {
        if (!isDeletable(id)) {
            throw new BusinessRuleException("rule-has-history", id);
        }
        rules.delete(rule(id));
    }

    /**
     * Changes the amount of one period only (RF-22, RF-31).
     *
     * @param occurrenceId    occurrence
     * @param amount          amount of the period
     * @param expectedVersion version the client edited, or {@code null}
     * @return the occurrence
     */
    public Occurrence overrideAmount(long occurrenceId, Money amount, Long expectedVersion) {
        Occurrence occurrence = occurrence(occurrenceId);
        Versions.check(expectedVersion, occurrence.version());
        occurrence.overrideAmount(amount);
        return occurrences.saveAndFlush(occurrence);
    }

    /**
     * Marks an income received or an expense paid, or back to expected (RF-24, RF-32).
     *
     * @param occurrenceId occurrence
     * @param done         {@code true} for received or paid
     * @param date         receipt or payment date (today when {@code null})
     * @return the occurrence
     */
    public Occurrence markDone(long occurrenceId, boolean done, LocalDate date) {
        Occurrence occurrence = occurrence(occurrenceId);
        if (done) {
            occurrence.markDone(date != null ? date : LocalDate.now());
        } else {
            occurrence.markExpected();
        }
        return occurrences.saveAndFlush(occurrence);
    }

    /**
     * Links a card-paid fixed expense to the entry the user chose among the candidates (RN-30).
     *
     * @param occurrenceId occurrence
     * @param entryId      chosen entry
     * @return the occurrence
     */
    public Occurrence chooseLink(long occurrenceId, long entryId) {
        Occurrence occurrence = occurrence(occurrenceId);
        linker.choose(occurrence, entryId);
        return occurrence;
    }

    /**
     * Lists the fixed expenses with several candidate entries, shown in the review queue (RN-30).
     *
     * @return occurrences waiting for a choice
     */
    @Transactional(readOnly = true)
    public List<Occurrence> pendingLinks() {
        return occurrences.findByLinkCandidatesIsNotNullOrderByBillingPeriod();
    }

    /**
     * Loads an occurrence.
     *
     * @param id id
     * @return the occurrence
     */
    @Transactional(readOnly = true)
    public Occurrence occurrence(long id) {
        return occurrences.findById(id).orElseThrow(() -> new NotFoundException("occurrence", id));
    }

    /**
     * Tells whether a rule can be deleted: only while none of its periods was received or paid; otherwise it is
     * inactivated (RF-01 style).
     *
     * @param id rule
     * @return {@code true} if deletable
     */
    @Transactional(readOnly = true)
    public boolean isDeletable(long id) {
        return occurrences.findByRuleId(id).stream().noneMatch(o -> o.status() == OccurrenceStatus.DONE);
    }

    /**
     * Lists every rule of a kind.
     *
     * @param kind income or fixed expense
     * @return rules
     */
    @Transactional(readOnly = true)
    public List<RecurrenceRule> rules(RuleKind kind) {
        return rules.findByKind(kind);
    }

    /**
     * Loads a rule.
     *
     * @param id id
     * @return the rule
     */
    @Transactional(readOnly = true)
    public RecurrenceRule rule(long id) {
        return rules.findById(id).orElseThrow(() -> new NotFoundException("recurrence-rule", id));
    }

    private void checkMember(long memberId) {
        if (members.find(memberId).isEmpty()) {
            throw new BusinessRuleException("card-member-unknown", memberId);
        }
    }
}
