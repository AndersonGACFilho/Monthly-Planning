package com.planejamentomensal.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DescriptionNormalizerTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = ';', value = {
        "Posto Agronomiaporto Alegrebra; POSTOAGRONOMIAPORTOALEGREBRA",
        "Bmb    *Equatorialporto Alegrebra; BMBEQUATORIALPORTOALEGREBRA",
        "Redução Mensalidade - Plano Do Cartão; REDUCAOMENSALIDADEPLANODOCARTAO",
        "farmácia são joão; FARMACIASAOJOAO"
    })
    @DisplayName("RN-15: no accents, upper case, only letters and digits")
    void normalizes(String original, String expected) {
        assertThat(DescriptionNormalizer.normalize(original)).isEqualTo(expected);
    }

    @Test
    @DisplayName("RF-65: the NN/NN installment suffix is separated from the description")
    void splitsInstallment() {
        var split = DescriptionNormalizer.splitInstallment("MERCADOLIVRE*M 02/02");
        assertThat(split.text()).isEqualTo("MERCADOLIVRE*M");
        assertThat(split.number()).isEqualTo(2);
        assertThat(split.count()).isEqualTo(2);
        var single = DescriptionNormalizer.splitInstallment("POSTO AGRONOMIAPORTO AL");
        assertThat(single.number()).isNull();
        assertThat(single.text()).isEqualTo("POSTO AGRONOMIAPORTO AL");
        assertThat(DescriptionNormalizer.splitInstallment("LOJA 05/02").number()).isNull();
    }

    @Test
    @DisplayName("RN-16: same store when one normalized description is a prefix of the other, with 8+ characters")
    void sameStore() {
        assertThat(DescriptionNormalizer.sameStore("BMB *Equato", "Bmb *Equatorialporto Alegrebra")).isTrue();
        assertThat(DescriptionNormalizer.sameStore("MERCADOLIVRE*M", "Mercadolivre*mercadoljaragua Do Sbra")).isTrue();
        // NUUVEMN has 7 characters: below the RN-16 minimum, so it is not matched by description (open question Q18)
        assertThat(DescriptionNormalizer.sameStore("nuuvem *N", "Nuuvem *Nuuvemrio De Janeirbra")).isFalse();
        assertThat(DescriptionNormalizer.sameStore("BMB", "BMBEQUATORIAL")).isFalse();
        assertThat(DescriptionNormalizer.sameStore("POSTO AGRONOMIA", "POSTO BR3")).isFalse();
        assertThat(DescriptionNormalizer.commonStore("BMB *Equato", "Bmb *Equatorial")).contains("BMBEQUATO");
        assertThat(DescriptionNormalizer.commonStore("A", "B")).isEmpty();
    }
}
