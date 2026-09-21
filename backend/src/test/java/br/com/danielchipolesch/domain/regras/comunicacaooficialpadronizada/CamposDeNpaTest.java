package br.com.danielchipolesch.domain.regras.comunicacaooficialpadronizada;

import br.com.danielchipolesch.application.dtos.npaDtos.AssinaturaDaNpaDto;
import br.com.danielchipolesch.application.dtos.npaDtos.CamposDaNpaDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.CamposDaNpa;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.DocumentoCompartilhamento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import br.com.danielchipolesch.domain.entities.numeracaoDocumento.EspecieNormativa;
import br.com.danielchipolesch.domain.entities.usuario.PostoGraduacao;
import br.com.danielchipolesch.domain.entities.usuario.Usuario;
import br.com.danielchipolesch.domain.handlers.exceptions.InvalidInputException;
import br.com.danielchipolesch.domain.handlers.exceptions.ResourceCannotBeUpdatedException;
import br.com.danielchipolesch.domain.handlers.exceptions.ResourceNotFoundException;
import br.com.danielchipolesch.domain.regras.TipoDeEspecie;
import br.com.danielchipolesch.infrastructure.repositories.CamposDaNpaRepository;
import br.com.danielchipolesch.infrastructure.repositories.DocumentoCompartilhamentoRepository;
import br.com.danielchipolesch.infrastructure.repositories.DocumentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Campos que só a NPA tem (docs/dominio.md, "NPA"): setor emissor, local do fecho e blocos de assinatura em texto
// livre. Nascem com orientação entre colchetes; depois de publicada a NPA não muda mais (não há alteração).
class CamposDeNpaTest {

    private final Map<Long, CamposDaNpa> banco = new HashMap<>();
    private final Map<Long, Documento> documentos = new HashMap<>();
    private final Map<Long, List<DocumentoCompartilhamento>> coautorias = new HashMap<>();
    private CamposDeNpa campos;

    @BeforeEach
    void preparar() {
        var repositorio = mock(CamposDaNpaRepository.class);
        when(repositorio.findById(anyLong())).thenAnswer(inv -> Optional.ofNullable(banco.get(inv.<Long>getArgument(0))));
        when(repositorio.save(org.mockito.ArgumentMatchers.any(CamposDaNpa.class))).thenAnswer(inv -> {
            CamposDaNpa c = inv.getArgument(0);
            banco.put(c.getDocumentoId(), c);
            return c;
        });
        var documentoRepository = mock(DocumentoRepository.class);
        when(documentoRepository.findById(anyLong())).thenAnswer(inv -> Optional.ofNullable(documentos.get(inv.<Long>getArgument(0))));
        var compartilhamentos = mock(DocumentoCompartilhamentoRepository.class);
        when(compartilhamentos.findByDocumentoId(anyLong()))
                .thenAnswer(inv -> coautorias.getOrDefault(inv.<Long>getArgument(0), List.of()));
        campos = new CamposDeNpa(repositorio, documentoRepository, compartilhamentos, new ObjectMapper());
    }

    private Documento documento(long id, TipoDeEspecie tipo, SituacaoBcaEnum bca) {
        var especie = new EspecieNormativa();
        especie.setTipoDeEspecie(tipo);
        var doc = new Documento();
        doc.setId(id);
        doc.setEspecieNormativa(especie);
        doc.setSituacaoBca(bca);
        documentos.put(id, doc);
        return doc;
    }

    @Test
    void nasceComOrientacaoEmColchetesESemBlocosDeAssinaturaEscritos() {
        var doc = documento(1, TipoDeEspecie.COMUNICACAO_OFICIAL_PADRONIZADA, SituacaoBcaEnum.NAO_PUBLICADO);

        campos.criarPara(doc);
        var dto = campos.obter(1L);

        assertThat(dto.setorEmissor()).isEqualTo("[SETOR EMISSOR]");
        assertThat(dto.local()).isEqualTo("[Local]");
        // "Elaborado por" e "Aprovado por" não são escritos: saem do documento.
        assertThat(dto.assinaturas()).isEmpty();
    }

    @Test
    void asAssinaturasSaoTextoLivreComRotuloELinhas() {
        var doc = documento(1, TipoDeEspecie.COMUNICACAO_OFICIAL_PADRONIZADA, SituacaoBcaEnum.NAO_PUBLICADO);
        campos.criarPara(doc);

        campos.atualizar(1L, new CamposDaNpaDto("Divisão de Suporte Operacional", "Brasília",
                List.of(new AssinaturaDaNpaDto("Proposto por", List.of("Fulano de Tal", "Maj Av", "Chefe da Seção")),
                        new AssinaturaDaNpaDto("Visto", List.of("Beltrano")))));
        var dto = campos.obter(1L);

        assertThat(dto.setorEmissor()).isEqualTo("Divisão de Suporte Operacional");
        assertThat(dto.local()).isEqualTo("Brasília");
        assertThat(dto.assinaturas()).hasSize(2);
        assertThat(dto.assinaturas().get(0).linhas()).containsExactly("Fulano de Tal", "Maj Av", "Chefe da Seção");
        assertThat(dto.assinaturas().get(1).rotulo()).isEqualTo("Visto");
    }

    @Test
    void aOrdemDosBlocosDeAssinaturaEPreservada() {
        var doc = documento(1, TipoDeEspecie.COMUNICACAO_OFICIAL_PADRONIZADA, SituacaoBcaEnum.NAO_PUBLICADO);
        campos.criarPara(doc);

        campos.atualizar(1L, new CamposDaNpaDto("Setor", "Local", List.of(
                new AssinaturaDaNpaDto("Visto", List.of("A")),
                new AssinaturaDaNpaDto("Proposto por", List.of("B")),
                new AssinaturaDaNpaDto("Ciente", List.of("C")))));

        assertThat(campos.obter(1L).assinaturas()).extracting(AssinaturaDaNpaDto::rotulo)
                .containsExactly("Visto", "Proposto por", "Ciente");
    }

    @Test
    void osRotulosDosBlocosAutomaticosNaoPodemSerEscritos() {
        var doc = documento(1, TipoDeEspecie.COMUNICACAO_OFICIAL_PADRONIZADA, SituacaoBcaEnum.NAO_PUBLICADO);
        campos.criarPara(doc);

        assertThatThrownBy(() -> campos.atualizar(1L, new CamposDaNpaDto("Setor", "Local",
                List.of(new AssinaturaDaNpaDto("Elaborado por", List.of("Fulano"))))))
                .isInstanceOf(InvalidInputException.class).hasMessageContaining("automaticamente");
        assertThatThrownBy(() -> campos.atualizar(1L, new CamposDaNpaDto("Setor", "Local",
                List.of(new AssinaturaDaNpaDto("aprovado por:", List.of("Beltrano"))))))
                .isInstanceOf(InvalidInputException.class);
    }

    private static Usuario usuario(long id, String nome, String bigrama) {
        var u = new Usuario();
        u.setId(id);
        u.setNome(nome);
        if (bigrama != null) {
            var posto = new PostoGraduacao();
            posto.setBigrama(bigrama);
            u.setPostoGraduacao(posto);
        }
        return u;
    }

    private static DocumentoCompartilhamento coautoria(long id, Usuario usuario) {
        var c = new DocumentoCompartilhamento();
        c.setId(id);
        c.setUsuario(usuario);
        return c;
    }

    @Test
    void elaboradoPorTrazOAutorETodosOsCoautoresNaOrdemDeInclusao() {
        var doc = documento(1, TipoDeEspecie.COMUNICACAO_OFICIAL_PADRONIZADA, SituacaoBcaEnum.NAO_PUBLICADO);
        doc.setAutor(usuario(1, "Fulano de Tal", "Cel"));
        coautorias.put(1L, List.of(coautoria(11, usuario(3, "Cicrano Souza", "Cap")),
                coautoria(10, usuario(2, "Beltrano Lima", "Maj")),
                coautoria(12, usuario(4, "Maria Servidora", null))));
        campos.criarPara(doc);

        assertThat(campos.obter(1L).elaboradoPor())
                .containsExactly("Cel FULANO DE TAL", "Maj BELTRANO LIMA", "Cap CICRANO SOUZA", "MARIA SERVIDORA");
    }

    @Test
    void oAutorQueTambemEhCoautorNaoApareceDuasVezes() {
        var doc = documento(1, TipoDeEspecie.COMUNICACAO_OFICIAL_PADRONIZADA, SituacaoBcaEnum.NAO_PUBLICADO);
        var autor = usuario(1, "Fulano de Tal", "Cel");
        doc.setAutor(autor);
        coautorias.put(1L, List.of(coautoria(10, autor)));
        campos.criarPara(doc);

        assertThat(campos.obter(1L).elaboradoPor()).containsExactly("Cel FULANO DE TAL");
    }

    @Test
    void aprovadoPorSoTemONomeDepoisQueODocumentoEAprovado() {
        var doc = documento(1, TipoDeEspecie.COMUNICACAO_OFICIAL_PADRONIZADA, SituacaoBcaEnum.NAO_PUBLICADO);
        doc.setAutor(usuario(1, "Fulano de Tal", "Cel"));
        campos.criarPara(doc);

        // Antes de enviar para revisão, ninguém foi escolhido.
        assertThat(campos.obter(1L).aprovadoPor()).isEmpty();

        // Enviado para revisão: já há um revisor escolhido, mas ele ainda não aprovou -- o nome não aparece.
        doc.setRevisorAtribuido(usuario(9, "Ana Aprovadora", "Brig"));
        assertThat(campos.obter(1L).aprovadoPor()).isEmpty();

        // Aprovado: agora é ele quem aparece.
        doc.setDtAprovacao(java.sql.Timestamp.valueOf("2026-03-12 10:00:00"));
        assertThat(campos.obter(1L).aprovadoPor()).containsExactly("Brig ANA APROVADORA");
    }

    @Test
    void semAssinaturasAceitaListaVazia() {
        var doc = documento(1, TipoDeEspecie.COMUNICACAO_OFICIAL_PADRONIZADA, SituacaoBcaEnum.NAO_PUBLICADO);
        campos.criarPara(doc);

        campos.atualizar(1L, new CamposDaNpaDto("Setor", "Local", null));

        assertThat(campos.obter(1L).assinaturas()).isEmpty();
    }

    @Test
    void setorOuLocalEmBrancoSaoRecusados() {
        var doc = documento(1, TipoDeEspecie.COMUNICACAO_OFICIAL_PADRONIZADA, SituacaoBcaEnum.NAO_PUBLICADO);
        campos.criarPara(doc);

        assertThatThrownBy(() -> campos.atualizar(1L, new CamposDaNpaDto("  ", "Local", List.of())))
                .isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> campos.atualizar(1L, new CamposDaNpaDto("Setor", "", List.of())))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void depoisDePublicadaOuRevogadaANpaNaoMudaMais() {
        var publicada = documento(1, TipoDeEspecie.COMUNICACAO_OFICIAL_PADRONIZADA, SituacaoBcaEnum.PUBLICADO);
        var revogada = documento(2, TipoDeEspecie.COMUNICACAO_OFICIAL_PADRONIZADA, SituacaoBcaEnum.REVOGADO);
        campos.criarPara(publicada);
        campos.criarPara(revogada);

        assertThatThrownBy(() -> campos.atualizar(1L, new CamposDaNpaDto("Setor", "Local", List.of())))
                .isInstanceOf(ResourceCannotBeUpdatedException.class).hasMessageContaining("crie outra");
        assertThatThrownBy(() -> campos.atualizar(2L, new CamposDaNpaDto("Setor", "Local", List.of())))
                .isInstanceOf(ResourceCannotBeUpdatedException.class);
    }

    @Test
    void umDocumentoQueNaoENpaNaoTemEsses() {
        documento(1, TipoDeEspecie.CONVENCIONAL, SituacaoBcaEnum.NAO_PUBLICADO);

        assertThatThrownBy(() -> campos.obter(1L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> campos.atualizar(1L, new CamposDaNpaDto("Setor", "Local", List.of())))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void documentoInexistenteEhNaoEncontrado() {
        assertThatThrownBy(() -> campos.obter(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void aCopiaLevaOsMesmosCamposDoOriginal() {
        var original = documento(1, TipoDeEspecie.COMUNICACAO_OFICIAL_PADRONIZADA, SituacaoBcaEnum.NAO_PUBLICADO);
        var copia = documento(2, TipoDeEspecie.COMUNICACAO_OFICIAL_PADRONIZADA, SituacaoBcaEnum.NAO_PUBLICADO);
        campos.criarPara(original);
        campos.atualizar(1L, new CamposDaNpaDto("Setor X", "Local Y",
                List.of(new AssinaturaDaNpaDto("Visto", List.of("Fulano")))));

        campos.copiar(original, copia);

        var dto = campos.obter(2L);
        assertThat(dto.setorEmissor()).isEqualTo("Setor X");
        assertThat(dto.local()).isEqualTo("Local Y");
        assertThat(dto.assinaturas()).hasSize(1);
    }

    @Test
    void copiarUmOriginalSemCamposCriaOsIniciais() {
        var original = documento(1, TipoDeEspecie.COMUNICACAO_OFICIAL_PADRONIZADA, SituacaoBcaEnum.NAO_PUBLICADO);
        var copia = documento(2, TipoDeEspecie.COMUNICACAO_OFICIAL_PADRONIZADA, SituacaoBcaEnum.NAO_PUBLICADO);

        campos.copiar(original, copia);

        assertThat(campos.obter(2L).setorEmissor()).isEqualTo("[SETOR EMISSOR]");
    }
}
