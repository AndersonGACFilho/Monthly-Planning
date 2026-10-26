package com.planejamentomensal.card.domain;

import com.planejamentomensal.shared.domain.BillingPeriod;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Credit card with its finals, holder, default member and closing/due cycles.
 *
 * <p>Implements RF-02 (default member), RF-03 (finals, holder, days), RF-04 (cycles valid from a billing
 * period, past bills untouched) and RF-05 (additional cards grouped under the primary card).</p>
 *
 * @since 1.0
 */
@Entity
@Table(name = "card")
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Bank bank;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "card_final", joinColumns = @JoinColumn(name = "card_id"))
    @Column(name = "last_digits", nullable = false)
    @OrderBy
    private List<String> lastDigits = new ArrayList<>();

    @Column(name = "holder_name", nullable = false)
    private String holderName;

    @Column(name = "primary_card_id")
    private Long primaryCardId;

    @Column(name = "default_member_id", nullable = false)
    private long defaultMemberId;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "card_id", nullable = false)
    private List<CardCycle> cycles = new ArrayList<>();

    @Version
    private long version;

    /**
     * For JPA.
     */
    protected Card() {
    }

    /**
     * Creates an active card.
     *
     * @param details    card data
     * @param firstCycle closing and due day; ignored for additional cards, which follow the primary card
     */
    public Card(CardDetails details, CardCycle firstCycle) {
        apply(details);
        if (primaryCardId == null) {
            cycles.add(Objects.requireNonNull(firstCycle, "firstCycle"));
        }
    }

    /**
     * Replaces the editable data.
     *
     * @param details new data
     */
    public void update(CardDetails details) {
        apply(details);
    }

    private void apply(CardDetails details) {
        if (details.lastDigits().isEmpty() || details.lastDigits().stream().anyMatch(d -> !d.matches("\\d{4}"))) {
            throw new IllegalArgumentException("Each card final has 4 digits and a card has at least one");
        }
        this.nickname = details.nickname();
        this.bank = details.bank();
        this.lastDigits = new ArrayList<>(details.lastDigits().stream().distinct().toList());
        this.holderName = details.holderName();
        this.primaryCardId = details.primaryCardId();
        this.defaultMemberId = details.defaultMemberId();
    }

    /**
     * Adds or replaces the cycle valid from a billing period (RF-04). Periods before it keep their cycle.
     *
     * @param cycle new cycle
     * @throws IllegalStateException if this is an additional card, which follows the primary card's cycle
     */
    public void changeCycle(CardCycle cycle) {
        if (isAdditional()) {
            throw new IllegalStateException("Additional cards follow the primary card's cycle");
        }
        cycles.removeIf(existing -> existing.validFrom().equals(cycle.validFrom()));
        cycles.add(cycle);
    }

    /**
     * Returns the cycle valid in a billing period: the one with the latest {@code validFrom} not after it.
     *
     * @param period billing period
     * @return the cycle, or empty for additional cards or periods before the first cycle
     */
    public Optional<CardCycle> cycleFor(BillingPeriod period) {
        return cycles.stream()
                .filter(cycle -> cycle.validFrom().compareTo(period) <= 0)
                .max(Comparator.comparing(CardCycle::validFrom));
    }

    /**
     * Tells whether this is an additional card (RF-05).
     *
     * @return {@code true} if it has a primary card
     */
    public boolean isAdditional() {
        return primaryCardId != null;
    }

    /**
     * Activates or inactivates the card.
     *
     * @param newActive new state
     */
    public void setActive(boolean newActive) {
        this.active = newActive;
    }

    /**
     * Returns the id.
     *
     * @return id
     */
    public Long id() {
        return id;
    }

    /**
     * Returns the nickname.
     *
     * @return nickname
     */
    public String nickname() {
        return nickname;
    }

    /**
     * Returns the issuer.
     *
     * @return bank
     */
    public Bank bank() {
        return bank;
    }

    /**
     * Returns the card finals.
     *
     * @return finals, 4 digits each
     */
    public List<String> lastDigits() {
        return List.copyOf(lastDigits);
    }

    /**
     * Returns the holder name as printed on the bill.
     *
     * @return holder name
     */
    public String holderName() {
        return holderName;
    }

    /**
     * Returns the primary card of an additional card.
     *
     * @return primary card id, or {@code null}
     */
    public Long primaryCardId() {
        return primaryCardId;
    }

    /**
     * Returns the default member (RF-02).
     *
     * @return member id
     */
    public long defaultMemberId() {
        return defaultMemberId;
    }

    /**
     * Tells whether the card is active.
     *
     * @return active flag
     */
    public boolean active() {
        return active;
    }

    /**
     * Returns every cycle, oldest first.
     *
     * @return cycles
     */
    public List<CardCycle> cycles() {
        return cycles.stream().sorted(Comparator.comparing(CardCycle::validFrom)).toList();
    }

    /**
     * Returns the optimistic lock version.
     *
     * @return version
     */
    public long version() {
        return version;
    }
}
