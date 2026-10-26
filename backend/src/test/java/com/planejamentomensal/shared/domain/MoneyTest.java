package com.planejamentomensal.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MoneyTest {

    private static List<BigDecimal> weights(String... values) {
        return java.util.Arrays.stream(values).map(BigDecimal::new).toList();
    }

    @Test
    @DisplayName("RNF-01: amounts keep exact cents and never use floating point")
    void exactCents() {
        // Given / When
        Money sum = Money.of("0.10").plus(Money.of("0.20"));
        // Then
        assertThat(sum).isEqualTo(Money.of("0.30"));
        assertThat(sum.cents()).isEqualTo(30);
        assertThat(Money.ofCents(123456)).hasToString("1234.56");
        assertThat(Money.of("1.005").amount()).isEqualByComparingTo("1.00");
    }

    @Test
    @DisplayName("RNF-01: arithmetic, sign and comparison")
    void arithmetic() {
        Money a = Money.of("48.02");
        assertThat(a.minus(Money.of("0.02"))).isEqualTo(Money.of("48.00"));
        assertThat(a.times(3)).isEqualTo(Money.of("144.06"));
        assertThat(a.negate().isNegative()).isTrue();
        assertThat(a.negate().abs()).isEqualTo(a);
        assertThat(Money.ZERO.isZero()).isTrue();
        assertThat(a.compareTo(Money.of("48.01"))).isPositive();
        assertThat(Money.of("100").percent(new BigDecimal("33.33"))).isEqualTo(Money.of("33.33"));
    }

    @Test
    @DisplayName("RN-18: amounts within R$ 0,02 are close")
    void closeTo() {
        Money tolerance = Money.of("0.02");
        assertThat(Money.of("48.02").isCloseTo(Money.of("48.01"), tolerance)).isTrue();
        assertThat(Money.of("48.04").isCloseTo(Money.of("48.01"), tolerance)).isFalse();
    }

    @ParameterizedTest(name = "{0} split {1} -> {2}")
    @CsvSource(delimiter = ';', value = {
        "100.00; 50,50; 50.00,50.00",
        "100.00; 1,1,1; 33.34,33.33,33.33",
        "0.05; 1,1; 0.03,0.02",
        "-10.00; 1,1,1; -3.34,-3.33,-3.33",
        "276.77; 70,30; 193.74,83.03"
    })
    @DisplayName("RF-26: allocation never loses cents")
    void allocateKeepsTotal(String amount, String weightList, String expected) {
        // Given
        Money total = Money.of(amount);
        // When
        List<Money> shares = total.allocate(weights(weightList.split(",")));
        // Then
        assertThat(shares).extracting(Money::toString).containsExactly(expected.split(","));
        assertThat(shares.stream().reduce(Money.ZERO, Money::plus)).isEqualTo(total);
    }

    @Test
    @DisplayName("RF-26: invalid weights are rejected")
    void invalidWeights() {
        assertThatThrownBy(() -> Money.of("1").allocate(List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Money.of("1").allocate(weights("0", "0"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Money.of("1").allocate(weights("-1", "2"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("RNF-01: converter stores cents")
    void converter() {
        MoneyConverter converter = new MoneyConverter();
        assertThat(converter.convertToDatabaseColumn(Money.of("12.34"))).isEqualTo(1234L);
        assertThat(converter.convertToEntityAttribute(-50L)).isEqualTo(Money.of("-0.50"));
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
