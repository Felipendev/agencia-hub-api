package com.agenciahub.api.domain.referrals;

import com.agenciahub.api.domain.referrals.Referrals.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReferralsTest {

    private static final LocalDate TODAY = LocalDate.parse("2026-10-21");
    private static final UUID A = UUID.randomUUID(), B = UUID.randomUUID();

    private static ReferralInput lead(String phone, String email) {
        return new ReferralInput(Product.TRAVEL, A, null, "  Bia Souza ", phone, email, TODAY, Channel.WHATSAPP, null, null, " ", null);
    }

    @Test
    void normalizesLeadContact() {
        ReferralInput out = Referrals.normalize(lead(" +55 (85) 99999-1234 ", " Bia@Exemplo.COM "), TODAY);
        assertThat(out.referredName()).isEqualTo("Bia Souza");
        assertThat(out.referredPhone()).isEqualTo("+5585999991234");
        assertThat(out.referredEmail()).isEqualTo("bia@exemplo.com");
        assertThat(out.notes()).isNull();
        assertThat(Referrals.phoneKey(out.referredPhone())).isEqualTo(Referrals.phoneKey("(85) 99999-1234")).isEqualTo("99991234");
    }

    @Test
    void crmReferredDropsTypedContact() {
        ReferralInput in = new ReferralInput(Product.MILES, A, B, "Digitado", "123", "x", TODAY, Channel.EVENT, null, null, null, null);
        ReferralInput out = Referrals.normalize(in, TODAY);
        assertThat(out.referredCustomerId()).isEqualTo(B);
        assertThat(out.referredName()).isNull();
        assertThat(out.referredPhone()).isNull();
    }

    @Test
    void rejectsInvalidInput() {
        assertThatThrownBy(() -> Referrals.normalize(new ReferralInput(null, A, B, null, null, null, TODAY, Channel.EVENT, null, null, null, null), TODAY))
                .hasMessage("Informe o produto da indicação: Viagens ou Milhas.");
        assertThatThrownBy(() -> Referrals.normalize(new ReferralInput(Product.TRAVEL, A, A, null, null, null, TODAY, Channel.EVENT, null, null, null, null), TODAY))
                .hasMessage("A pessoa indicada não pode ser a mesma que indicou.");
        assertThatThrownBy(() -> Referrals.normalize(lead(null, null), TODAY)).hasMessage("Informe telefone ou e-mail da pessoa indicada.");
        assertThatThrownBy(() -> Referrals.normalize(lead("123", null), TODAY)).hasMessage("Telefone da pessoa indicada inválido.");
        assertThatThrownBy(() -> Referrals.normalize(lead(null, "sem-arroba"), TODAY)).hasMessage("E-mail da pessoa indicada inválido.");
        assertThatThrownBy(() -> Referrals.normalize(new ReferralInput(Product.TRAVEL, A, B, null, null, null, TODAY.plusDays(1), Channel.EVENT, null, null, null, null), TODAY))
                .hasMessage("A data da indicação não pode estar no futuro.");
        assertThatThrownBy(() -> Referrals.normalize(new ReferralInput(Product.TRAVEL, A, B, null, null, null, TODAY, Channel.OTHER, " ", null, null, null), TODAY))
                .hasMessage("Descreva a origem quando escolher “Outro”.");
    }

    private static StatusInput to(Status s, String reason) { return new StatusInput(s, reason, null, null, 1); }

    @Test
    void lifecycleRequiresReasonsAndCrmForConversion() {
        Referrals.requireTransition(Status.RECEIVED, to(Status.CONTACTED, null), Product.TRAVEL, false, TODAY, TODAY);
        assertThatThrownBy(() -> Referrals.requireTransition(Status.CONTACTED, new StatusInput(Status.CONVERTED, null, TODAY, null, 1), Product.TRAVEL, false, TODAY, TODAY))
                .hasMessage("Vincule a pessoa indicada a um cadastro antes de marcar como convertida.");
        assertThatThrownBy(() -> Referrals.requireTransition(Status.CONTACTED, new StatusInput(Status.CONVERTED, null, null, null, 1), Product.TRAVEL, true, TODAY, TODAY))
                .hasMessage("Informe a data da conversão.");
        assertThatThrownBy(() -> Referrals.requireTransition(Status.CONTACTED, new StatusInput(Status.CONVERTED, null, TODAY.minusDays(3), null, 1), Product.TRAVEL, true, TODAY, TODAY.minusDays(1)))
                .hasMessage("A conversão não pode ser anterior à indicação.");
        assertThatThrownBy(() -> Referrals.requireTransition(Status.CONTACTED, new StatusInput(Status.CONVERTED, null, TODAY, UUID.randomUUID(), 1), Product.MILES, true, TODAY, TODAY))
                .hasMessage("A venda só pode ser vinculada em indicações de Viagens.");
        Referrals.requireTransition(Status.CONTACTED, new StatusInput(Status.CONVERTED, null, TODAY, UUID.randomUUID(), 1), Product.TRAVEL, true, TODAY, TODAY);

        assertThatThrownBy(() -> Referrals.requireTransition(Status.CONTACTED, to(Status.NOT_CONVERTED, " "), Product.TRAVEL, false, TODAY, TODAY))
                .hasMessage("Informe por que a indicação não foi convertida.");
        assertThatThrownBy(() -> Referrals.requireTransition(Status.CONVERTED, to(Status.CONTACTED, null), Product.TRAVEL, true, TODAY, TODAY))
                .hasMessage("Informe o motivo para reabrir a indicação.");
        assertThatThrownBy(() -> Referrals.requireTransition(Status.CONVERTED, to(Status.NOT_CONVERTED, "x"), Product.TRAVEL, true, TODAY, TODAY))
                .hasMessage("Reabra a indicação como em contato antes de alterar o resultado.");
        assertThatThrownBy(() -> Referrals.requireTransition(Status.CANCELLED, to(Status.CONTACTED, "x"), Product.TRAVEL, true, TODAY, TODAY))
                .hasMessage("Transição não permitida.");
        assertThatThrownBy(() -> Referrals.requireTransition(Status.CANCELLED, to(Status.RECEIVED, null), Product.TRAVEL, true, TODAY, TODAY))
                .hasMessage("Informe o motivo para reativar a indicação.");
        assertThatThrownBy(() -> Referrals.requireTransition(Status.RECEIVED, new StatusInput(Status.CONTACTED, null, TODAY, null, 1), Product.TRAVEL, true, TODAY, TODAY))
                .hasMessage("Data de conversão e venda só são informadas ao marcar como convertida.");
        Referrals.requireTransition(Status.CONVERTED, to(Status.CANCELLED, "Cadastro duplicado"), Product.TRAVEL, true, TODAY, TODAY);
        Referrals.requireTransition(Status.CANCELLED, to(Status.RECEIVED, "Cancelada por engano"), Product.TRAVEL, true, TODAY, TODAY);
    }
}
