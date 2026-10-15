package intraer.fablegis.domain.regras;

import intraer.fablegis.domain.entities.usuario.OrganizacaoMilitar;
import intraer.fablegis.domain.entities.usuario.PapelEnum;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.domain.regras.comunicacaooficialpadronizada.CicloDeVidaDeNpa;
import intraer.fablegis.domain.regras.comunicacaooficialpadronizada.RegrasDeComunicacaoOficialPadronizada;
import intraer.fablegis.domain.regras.convencional.CicloDeVidaDeEspecieConvencional;
import intraer.fablegis.domain.regras.convencional.RegrasDeEspecieConvencional;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Quem publica cada espécie (docs/ciclo-de-vida.md): as convencionais, o Publicador do BCA (CENDOC, de qualquer OM); a NPA, que é de uso
// interno da OM, o Publicador do Boletim Interno da PRÓPRIA OM do documento. Qualquer OM continua VENDO qualquer documento.
class PublicacaoPorEspecieTest {

    private static final long OM_DO_DOCUMENTO = 1L;
    private static final long OUTRA_OM = 99L;

    private final RegrasDaEspecieNormativa convencional = new RegrasDeEspecieConvencional(
            null, null, null, null, null, null, null, null, null, new CicloDeVidaDeEspecieConvencional());
    private final RegrasDaEspecieNormativa npa = new RegrasDeComunicacaoOficialPadronizada(
            null, null, null, null, null, null, null, null, null, new CicloDeVidaDeNpa());

    private static Usuario pessoa(long omId, PapelEnum... papeis) {
        var om = new OrganizacaoMilitar();
        om.setId(omId);
        var u = new Usuario();
        u.setOm(om);
        var conjunto = EnumSet.noneOf(PapelEnum.class);
        conjunto.addAll(List.of(papeis));
        u.setPapeis(conjunto);
        return u;
    }

    @Test
    void cadaEspecieDizQuemAPublicaEOndeVale() {
        assertThat(convencional.cicloDeVida().papelQuePublica()).isEqualTo(PapelEnum.PUB_BCA);
        assertThat(convencional.deUsoInternoDaOm()).isFalse();
        assertThat(npa.cicloDeVida().papelQuePublica()).isEqualTo(PapelEnum.PUB_BI);
        assertThat(npa.deUsoInternoDaOm()).isTrue();
    }

    @Test
    void oPublicadorDoBcaPublicaAsConvencionaisDeQualquerOm() {
        var doBca = pessoa(OM_DO_DOCUMENTO, PapelEnum.PUB_BCA);
        assertThat(convencional.podePublicar(doBca, OM_DO_DOCUMENTO)).isTrue();
        assertThat(convencional.podePublicar(doBca, OUTRA_OM)).isTrue();
    }

    @Test
    void oPublicadorDoBcaNaoPublicaNpa() {
        assertThat(npa.podePublicar(pessoa(OM_DO_DOCUMENTO, PapelEnum.PUB_BCA), OM_DO_DOCUMENTO)).isFalse();
    }

    @Test
    void oPublicadorDoBoletimInternoPublicaAsNpasSoDaPropriaOm() {
        var doBi = pessoa(OM_DO_DOCUMENTO, PapelEnum.PUB_BI);
        assertThat(npa.podePublicar(doBi, OM_DO_DOCUMENTO)).isTrue();
        assertThat(npa.podePublicar(doBi, OUTRA_OM)).isFalse();
    }

    @Test
    void oPublicadorDoBoletimInternoNaoPublicaEspecieConvencional() {
        assertThat(convencional.podePublicar(pessoa(OM_DO_DOCUMENTO, PapelEnum.PUB_BI), OM_DO_DOCUMENTO)).isFalse();
    }

    @Test
    void ninguemSemOPapelPublica() {
        var semPapel = pessoa(OM_DO_DOCUMENTO, PapelEnum.ELB, PapelEnum.APR_OM, PapelEnum.ADM_OM);
        assertThat(convencional.podePublicar(semPapel, OM_DO_DOCUMENTO)).isFalse();
        assertThat(npa.podePublicar(semPapel, OM_DO_DOCUMENTO)).isFalse();
    }

    @Test
    void oRegistroSeparaOsTiposDeUsoInternoDosDemais() {
        var regras = new RegrasDasEspecies(List.of(convencional, npa));

        assertThat(regras.tipos(true)).containsExactly(TipoDeEspecie.COMUNICACAO_OFICIAL_PADRONIZADA);
        assertThat(regras.tipos(false)).containsExactly(TipoDeEspecie.CONVENCIONAL);
    }
}
