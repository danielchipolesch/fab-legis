package intraer.fablegis.domain.regras;

// Quem pode CRIAR documentos de uma espécie. As Espécies Convencionais (MCA, NSCA, ICA, ROCA, DCA...) só podem ser criadas pelas
// OMs que o administrador (CENDOC) habilitou para a espécie (HabilitacaoDeCriacaoService); as Comunicações Oficiais
// Padronizadas (a NPA) são criadas por qualquer OM. Só a CRIAÇÃO depende disto: consultar documentos, ser coautor e editar um
// documento em que se é coautor não dependem da OM estar habilitada.
public interface RegraDeHabilitacaoDaOm {

    // true: a OM precisa estar habilitada para a espécie; false: qualquer OM cria.
    boolean exigeHabilitacaoDaOm();
}
