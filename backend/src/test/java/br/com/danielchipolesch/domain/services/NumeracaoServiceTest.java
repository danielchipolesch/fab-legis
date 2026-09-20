package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ElementoEmendaStatusEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import br.com.danielchipolesch.domain.services.NumeracaoService.ElementoNumeracao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum.ARTIGO;
import static br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum.CAPITULO;
import static br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum.PARAGRAFO;
import static br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum.PARAGRAFO_UNICO;
import static br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum.SECAO_NORMATIVA;
import static org.assertj.core.api.Assertions.assertThat;

// Regras de numeração do Decreto nº 12.002/2024 art. 9º e da LC 95/1998 (vedação
// de renumeração): ver docs/dominio.md. Este serviço não tem dependências, então
// é instanciado direto -- sem Spring, sem banco.
class NumeracaoServiceTest {

    private final NumeracaoService service = new NumeracaoService();

    // ─── Fixtures ────────────────────────────────────────────────────────────────

    private static ItemAnexoParteNormativaResponseDto no(long id, ItemAnexoParteNormativaTipoEnum tipo,
                                                          ItemAnexoParteNormativaResponseDto... filhos) {
        return no(id, tipo, false, ElementoEmendaStatusEnum.INALTERADO, filhos);
    }

    private static ItemAnexoParteNormativaResponseDto incluido(long id, ItemAnexoParteNormativaTipoEnum tipo,
                                                                ItemAnexoParteNormativaResponseDto... filhos) {
        return no(id, tipo, true, ElementoEmendaStatusEnum.INCLUIDO, filhos);
    }

    private static ItemAnexoParteNormativaResponseDto no(long id, ItemAnexoParteNormativaTipoEnum tipo,
                                                          boolean incluidoPorEmenda, ElementoEmendaStatusEnum status,
                                                          ItemAnexoParteNormativaResponseDto... filhos) {
        return new ItemAnexoParteNormativaResponseDto(id, null, tipo, null, null, null, null, status,
                null, null, null, null, null, null, incluidoPorEmenda, null, null, List.of(filhos));
    }

    private static ItemAnexoParteNormativaResponseDto artigo(long id) { return no(id, ARTIGO); }

    private Map<Long, ElementoNumeracao> calcular(ItemAnexoParteNormativaResponseDto... raiz) {
        return service.calcular(List.of(raiz));
    }

    private static String label(Map<Long, ElementoNumeracao> numeracao, long id) {
        return numeracao.get(id).label();
    }

    // ─── Formatação (art. 9º do Decreto 12.002/2024) ─────────────────────────────

    @ParameterizedTest(name = "{0} em romano é {1}")
    @CsvSource({"1,I", "4,IV", "9,IX", "14,XIV", "40,XL", "90,XC", "400,CD", "1994,MCMXCIV"})
    void numeroRomano(int numero, String esperado) {
        assertThat(NumeracaoService.toRoman(numero)).isEqualTo(esperado);
    }

    @Test
    void romanoDeZeroOuNegativoEVazio() {
        assertThat(NumeracaoService.toRoman(0)).isEmpty();
        assertThat(NumeracaoService.toRoman(-3)).isEmpty();
    }

    @ParameterizedTest(name = "artigo {0} é \"{1}\"")
    @CsvSource({"1,1º", "9,9º", "10,10.", "12,12.", "1024,'1.024.'"})
    void artigoOrdinalAteNoveECardinalAPartirDeDez(int numero, String esperado) {
        assertThat(NumeracaoService.ordinalOrCardinal(numero)).isEqualTo(esperado);
    }

    @ParameterizedTest(name = "número {0} sem ponto final é \"{1}\"")
    @CsvSource({"1,1º", "9,9º", "10,10", "1024,'1.024'"})
    void numeroSemPontoFinal(int numero, String esperado) {
        assertThat(NumeracaoService.fmtNum(numero)).isEqualTo(esperado);
    }

    @Test
    void separadorDeMilharNaoSeAplicaAAnosNemAParagrafosDeAte999() {
        assertThat(NumeracaoService.comSeparadorMilhar(999)).isEqualTo("999");
        assertThat(NumeracaoService.comSeparadorMilhar(1000)).isEqualTo("1.000");
        assertThat(NumeracaoService.comSeparadorMilhar(1234567)).isEqualTo("1.234.567");
    }

    @ParameterizedTest(name = "artigo {0} com letra {1} é \"{2}\"")
    @CsvSource({"5,A,5º-A", "9,B,9º-B", "10,A,10-A.", "12,C,12-C.", "1024,B,'1.024-B.'"})
    void sufixoDeLetraMigraOPontoDoCardinalParaODepoisDaLetra(int numero, String letra, String esperado) {
        assertThat(NumeracaoService.comSufixoLetra(numero, letra)).isEqualTo(esperado);
    }

    @Test
    void semLetraSufixoEOOrdinalOuCardinalNormal() {
        assertThat(NumeracaoService.comSufixoLetra(3, null)).isEqualTo("3º");
        assertThat(NumeracaoService.comSufixoLetra(11, null)).isEqualTo("11.");
    }

    @Test
    void letrasDeSufixoComecamEmA() {
        assertThat(NumeracaoService.letterFor(0)).isEqualTo("A");
        assertThat(NumeracaoService.letterFor(1)).isEqualTo("B");
        assertThat(NumeracaoService.toLetter(1)).isEqualTo("a");
        assertThat(NumeracaoService.toLetter(3)).isEqualTo("c");
    }

    // ─── Numeração sem emenda ────────────────────────────────────────────────────

    @Test
    void capitulosSaoNumeradosEmRomanoSequencialmente() {
        var numeracao = calcular(no(1, CAPITULO), no(2, CAPITULO), no(3, CAPITULO));

        assertThat(label(numeracao, 1)).isEqualTo("I");
        assertThat(label(numeracao, 2)).isEqualTo("II");
        assertThat(label(numeracao, 3)).isEqualTo("III");
    }

    @Test
    void secoesReiniciamEmCadaCapitulo() {
        var numeracao = calcular(
                no(1, CAPITULO, no(2, SECAO_NORMATIVA), no(3, SECAO_NORMATIVA)),
                no(4, CAPITULO, no(5, SECAO_NORMATIVA), no(6, SECAO_NORMATIVA)));

        assertThat(label(numeracao, 2)).isEqualTo("I");
        assertThat(label(numeracao, 3)).isEqualTo("II");
        assertThat(label(numeracao, 5)).isEqualTo("I");
        assertThat(label(numeracao, 6)).isEqualTo("II");
    }

    @Test
    void artigosTemNumeracaoContinuaPeloDocumentoInteiro() {
        var numeracao = calcular(
                no(1, CAPITULO, no(2, SECAO_NORMATIVA, artigo(3), artigo(4))),
                no(5, CAPITULO, artigo(6), no(7, SECAO_NORMATIVA, artigo(8))));

        assertThat(label(numeracao, 3)).isEqualTo("1º");
        assertThat(label(numeracao, 4)).isEqualTo("2º");
        assertThat(label(numeracao, 6)).isEqualTo("3º");
        assertThat(label(numeracao, 8)).isEqualTo("4º");
    }

    // Estrutura que todo documento novo recebe (CapitulosPadronizadosService).
    @Test
    void estruturaPadraoDaNsca53() {
        var numeracao = calcular(
                no(1, CAPITULO,
                        no(2, SECAO_NORMATIVA, artigo(3)),
                        no(4, SECAO_NORMATIVA, artigo(5))),
                no(6, CAPITULO, artigo(7)),
                no(8, CAPITULO, artigo(9)),
                no(10, CAPITULO,
                        no(11, SECAO_NORMATIVA, artigo(12)),
                        no(13, SECAO_NORMATIVA, artigo(14))));

        assertThat(List.of(1L, 6L, 8L, 10L)).extracting(id -> label(numeracao, id))
                .containsExactly("I", "II", "III", "IV");
        assertThat(List.of(2L, 4L, 11L, 13L)).extracting(id -> label(numeracao, id))
                .containsExactly("I", "II", "I", "II");
        assertThat(List.of(3L, 5L, 7L, 9L, 12L, 14L)).extracting(id -> label(numeracao, id))
                .containsExactly("1º", "2º", "3º", "4º", "5º", "6º");
    }

    @Test
    void artigoDeNumeroDezEmDianteUsaCardinalComPonto() {
        var artigos = new ArrayList<ItemAnexoParteNormativaResponseDto>();
        for (long i = 1; i <= 12; i++) artigos.add(artigo(i));
        var numeracao = service.calcular(List.of(no(100, CAPITULO, artigos.toArray(new ItemAnexoParteNormativaResponseDto[0]))));

        assertThat(label(numeracao, 9)).isEqualTo("9º");
        assertThat(label(numeracao, 10)).isEqualTo("10.");
        assertThat(label(numeracao, 12)).isEqualTo("12.");
    }

    @Test
    void artigosNaRaizSemAgrupamentoTambemSaoNumerados() {
        var numeracao = calcular(artigo(1), artigo(2));

        assertThat(label(numeracao, 1)).isEqualTo("1º");
        assertThat(label(numeracao, 2)).isEqualTo("2º");
    }

    @Test
    void numeroEOLabelEstaoNoResultado() {
        var numeracao = calcular(no(1, CAPITULO, artigo(2)));

        assertThat(numeracao.get(1L)).isEqualTo(new ElementoNumeracao(1, null, "I"));
        assertThat(numeracao.get(2L)).isEqualTo(new ElementoNumeracao(1, null, "1º"));
        assertThat(numeracao.get(1L).semNumero()).isFalse();
        assertThat(new ElementoNumeracao(0, null, "").semNumero()).isTrue();
    }

    // ─── Numeração com emenda (sufixo de letra, LC 95/1998) ──────────────────────

    @Test
    void artigoIncluidoEntreDoisAtivosRecebeLetraSemConsumirNumeracao() {
        var numeracao = calcular(no(1, CAPITULO, artigo(2), incluido(3, ARTIGO), artigo(4)));

        assertThat(label(numeracao, 2)).isEqualTo("1º");
        assertThat(label(numeracao, 3)).isEqualTo("1º-A");
        assertThat(numeracao.get(3L).numero()).isEqualTo(1);
        assertThat(numeracao.get(3L).letra()).isEqualTo("A");
        // O artigo seguinte não é deslocado: é a garantia central da vedação de renumeração.
        assertThat(label(numeracao, 4)).isEqualTo("2º");
    }

    @Test
    void variosArtigosIncluidosSeguidosRecebemLetrasSucessivas() {
        var numeracao = calcular(no(1, CAPITULO, artigo(2), incluido(3, ARTIGO), incluido(4, ARTIGO), artigo(5)));

        assertThat(label(numeracao, 3)).isEqualTo("1º-A");
        assertThat(label(numeracao, 4)).isEqualTo("1º-B");
        assertThat(label(numeracao, 5)).isEqualTo("2º");
    }

    @Test
    void artigoIncluidoAoFinalDaSequenciaRecebeNumeracaoNormal() {
        var numeracao = calcular(no(1, CAPITULO, artigo(2), incluido(3, ARTIGO)));

        assertThat(label(numeracao, 3)).isEqualTo("2º");
        assertThat(numeracao.get(3L).letra()).isNull();
    }

    @Test
    void letraDeArtigoIncluidoComNumeroDeDezEmDianteTemPontoDepoisDaLetra() {
        var artigos = new ArrayList<ItemAnexoParteNormativaResponseDto>();
        for (long i = 1; i <= 10; i++) artigos.add(artigo(i));
        artigos.add(incluido(50, ARTIGO));
        artigos.add(artigo(11));
        var numeracao = service.calcular(List.of(no(100, CAPITULO, artigos.toArray(new ItemAnexoParteNormativaResponseDto[0]))));

        assertThat(label(numeracao, 50)).isEqualTo("10-A.");
        assertThat(label(numeracao, 11)).isEqualTo("11.");
    }

    // A marca é permanente (incluidoPorEmenda), não o status ao vivo: alterar ou
    // revogar o artigo depois não pode fazê-lo perder a letra nem deslocar os demais.
    @Test
    void artigoIncluidoMantemALetraMesmoDepoisDeAlteradoOuRevogado() {
        for (var status : List.of(ElementoEmendaStatusEnum.ALTERADO, ElementoEmendaStatusEnum.REVOGADO)) {
            var numeracao = calcular(no(1, CAPITULO,
                    artigo(2),
                    no(3, ARTIGO, true, status),
                    artigo(4)));

            assertThat(label(numeracao, 3)).as("status %s", status).isEqualTo("1º-A");
            assertThat(label(numeracao, 4)).as("status %s", status).isEqualTo("2º");
        }
    }

    @Test
    void capituloIncluidoEntreDoisAtivosRecebeLetra() {
        var numeracao = calcular(no(1, CAPITULO), incluido(2, CAPITULO), no(3, CAPITULO));

        assertThat(label(numeracao, 1)).isEqualTo("I");
        assertThat(label(numeracao, 2)).isEqualTo("I-A");
        assertThat(label(numeracao, 3)).isEqualTo("II");
    }

    @Test
    void capituloIncluidoAoFinalRecebeNumeracaoNormal() {
        var numeracao = calcular(no(1, CAPITULO), incluido(2, CAPITULO));

        assertThat(label(numeracao, 2)).isEqualTo("II");
    }

    @Test
    void secaoIncluidaRecebeLetraLocalAoCapitulo() {
        var numeracao = calcular(no(1, CAPITULO,
                no(2, SECAO_NORMATIVA), incluido(3, SECAO_NORMATIVA), no(4, SECAO_NORMATIVA)));

        assertThat(label(numeracao, 2)).isEqualTo("I");
        assertThat(label(numeracao, 3)).isEqualTo("I-A");
        assertThat(label(numeracao, 4)).isEqualTo("II");
    }

    // Revogado mantém o número (LC 95/1998): revogar não abre vaga nem renumera, então o
    // elemento original revogado continua contando como "ativo" para quem foi incluído
    // antes dele. O numbering.js (frontend) segue a mesma regra e tem os mesmos cenários.
    @Test
    void artigoIncluidoAntesDeUmRevogadoRecebeLetraEORevogadoMantemONumero() {
        var revogado = no(4, ARTIGO, false, ElementoEmendaStatusEnum.REVOGADO);
        var numeracao = calcular(no(1, CAPITULO, artigo(2), incluido(3, ARTIGO), revogado));

        assertThat(label(numeracao, 3)).isEqualTo("1º-A");
        assertThat(label(numeracao, 4)).isEqualTo("2º");
    }

    @Test
    void capituloIncluidoAntesDeUmRevogadoRecebeLetraEORevogadoMantemONumero() {
        var revogado = no(3, CAPITULO, false, ElementoEmendaStatusEnum.REVOGADO);
        var numeracao = calcular(no(1, CAPITULO), incluido(2, CAPITULO), revogado);

        assertThat(label(numeracao, 2)).isEqualTo("I-A");
        assertThat(label(numeracao, 3)).isEqualTo("II");
    }

    @Test
    void secaoIncluidaAntesDeUmaRevogadaRecebeLetraEARevogadaMantemONumero() {
        var revogada = no(4, SECAO_NORMATIVA, false, ElementoEmendaStatusEnum.REVOGADO);
        var numeracao = calcular(no(1, CAPITULO, no(2, SECAO_NORMATIVA), incluido(3, SECAO_NORMATIVA), revogada));

        assertThat(label(numeracao, 3)).isEqualTo("I-A");
        assertThat(label(numeracao, 4)).isEqualTo("II");
    }

    // ─── Parágrafos: em vigor, nunca renumerados (Decreto 12.002/2024, art. 14, IV) ──

    private static List<String> rotulosDeParagrafos(ItemAnexoParteNormativaResponseDto... paragrafos) {
        return NumeracaoService.numerarParagrafos(List.of(paragrafos)).stream().map(ElementoNumeracao::label).toList();
    }

    @Test
    void paragrafoUnicoSozinhoPermaneceUnico() {
        assertThat(rotulosDeParagrafos(no(1, PARAGRAFO_UNICO))).containsExactly("Parágrafo único");
    }

    @Test
    void paragrafosSaoNumeradosEmSequencia() {
        assertThat(rotulosDeParagrafos(no(1, PARAGRAFO), no(2, PARAGRAFO), no(3, PARAGRAFO)))
                .containsExactly("§ 1º", "§ 2º", "§ 3º");
    }

    @Test
    void comMaisDeUmParagrafoOUnicoViraNumerado() {
        assertThat(rotulosDeParagrafos(no(1, PARAGRAFO_UNICO), no(2, PARAGRAFO))).containsExactly("§ 1º", "§ 2º");
    }

    @Test
    void paragrafoIncluidoEntreDoisEmVigorRecebeLetraSemDeslocarOsSeguintes() {
        assertThat(rotulosDeParagrafos(no(1, PARAGRAFO), incluido(2, PARAGRAFO), no(3, PARAGRAFO)))
                .containsExactly("§ 1º", "§ 1º-A", "§ 2º");
    }

    @Test
    void paragrafosIncluidosSeguidosRecebemLetrasSucessivas() {
        assertThat(rotulosDeParagrafos(no(1, PARAGRAFO), incluido(2, PARAGRAFO), incluido(3, PARAGRAFO), no(4, PARAGRAFO)))
                .containsExactly("§ 1º", "§ 1º-A", "§ 1º-B", "§ 2º");
    }

    @Test
    void paragrafoIncluidoAoFinalRecebeNumeracaoNormal() {
        assertThat(rotulosDeParagrafos(no(1, PARAGRAFO), incluido(2, PARAGRAFO))).containsExactly("§ 1º", "§ 2º");
    }

    @Test
    void letraDeParagrafoIncluidoAPartirDoDecimoTemPontoDepoisDaLetra() {
        var lista = new ArrayList<ItemAnexoParteNormativaResponseDto>();
        for (long i = 1; i <= 10; i++) lista.add(no(i, PARAGRAFO));
        lista.add(incluido(50, PARAGRAFO));
        lista.add(no(11, PARAGRAFO));

        var rotulos = rotulosDeParagrafos(lista.toArray(new ItemAnexoParteNormativaResponseDto[0]));

        assertThat(rotulos.get(10)).isEqualTo("§ 10-A.");
        assertThat(rotulos.get(11)).isEqualTo("§ 11.");
    }

    // Parágrafo NÃO pode ser renumerado: o revogado continua ocupando o número que tinha,
    // então o incluído antes dele recebe letra e o revogado não é empurrado para a frente.
    @Test
    void paragrafoIncluidoAntesDeUmRevogadoRecebeLetraEORevogadoMantemONumero() {
        var revogado = no(3, PARAGRAFO, false, ElementoEmendaStatusEnum.REVOGADO);

        assertThat(rotulosDeParagrafos(no(1, PARAGRAFO), incluido(2, PARAGRAFO), revogado))
                .containsExactly("§ 1º", "§ 1º-A", "§ 2º");
    }

    // A decisão usa a marca permanente (incluidoPorEmenda), não o status ao vivo: alterar
    // um parágrafo incluído depois não pode mudar o rótulo dos que vêm antes dele.
    @Test
    void alterarUmParagrafoIncluidoNaoMudaORotuloDosAnteriores() {
        var incluidoA = incluido(2, PARAGRAFO);
        var incluidoB = no(3, PARAGRAFO, true, ElementoEmendaStatusEnum.ALTERADO);

        assertThat(rotulosDeParagrafos(no(1, PARAGRAFO), incluidoA, incluidoB))
                .containsExactly("§ 1º", "§ 2º", "§ 3º");
    }

    // ─── Sumário: pontos e intervalos de artigos ─────────────────────────────────

    @Test
    void pontoFinalDeArtigoUsaLetraSemPontoFinal() {
        var artigos = new ArrayList<ItemAnexoParteNormativaResponseDto>();
        for (long i = 1; i <= 12; i++) artigos.add(artigo(i));
        artigos.add(incluido(50, ARTIGO));
        artigos.add(artigo(13));
        var raiz = no(100, CAPITULO, artigos.toArray(new ItemAnexoParteNormativaResponseDto[0]));
        var numeracao = service.calcular(List.of(raiz));

        assertThat(service.pontoFinalArtigo(artigo(1), numeracao)).isEqualTo("1º");
        assertThat(service.pontoFinalArtigo(artigo(12), numeracao)).isEqualTo("12");
        assertThat(service.pontoFinalArtigo(incluido(50, ARTIGO), numeracao)).isEqualTo("12-A");
    }

    @Test
    void pontoFinalDeElementoSemNumeracaoEVazio() {
        assertThat(service.pontoFinalArtigo(artigo(99), Map.of())).isEmpty();
    }

    @Test
    void intervaloDeArtigosDeUmCapitulo() {
        var cap = no(1, CAPITULO, artigo(2), artigo(3), artigo(4));
        var numeracao = service.calcular(List.of(cap));

        assertThat(service.intervaloArtigos(cap, List.of(cap), 0, numeracao)).isEqualTo("1º/3º");
    }

    @Test
    void intervaloDeUmSoArtigoNaoRepeteOPonto() {
        var cap = no(1, CAPITULO, artigo(2));
        var numeracao = service.calcular(List.of(cap));

        assertThat(service.intervaloArtigos(cap, List.of(cap), 0, numeracao)).isEqualTo("1º");
    }

    @Test
    void intervaloConsideraArtigosIrmaosAteOProximoAgrupamento() {
        var sec1 = no(1, SECAO_NORMATIVA);
        var lista = List.of(sec1, artigo(2), artigo(3), no(4, SECAO_NORMATIVA), artigo(5));
        var numeracao = service.calcular(lista);

        assertThat(service.intervaloArtigos(sec1, lista, 0, numeracao)).isEqualTo("1º/2º");
    }

    @Test
    void intervaloDeAgrupamentoSemArtigosEVazio() {
        var cap = no(1, CAPITULO);
        var numeracao = service.calcular(List.of(cap));

        assertThat(service.intervaloArtigos(cap, List.of(cap), 0, numeracao)).isEmpty();
    }

    @Test
    void reconheceSeExisteAgrupamentoNaLista() {
        assertThat(service.temAgrupamento(List.of(artigo(1), no(2, CAPITULO)))).isTrue();
        assertThat(service.temAgrupamento(List.of(artigo(1), artigo(2)))).isFalse();
        assertThat(service.temAgrupamento(null)).isFalse();
    }

    // ─── Parágrafo único que vira "§ 1º" (único caso de renumeração de parágrafo) ─

    private static ItemAnexoParteNormativaResponseDto par(long id, ItemAnexoParteNormativaTipoEnum tipo,
                                                          ElementoEmendaStatusEnum status, String clausulaEmenda,
                                                          String clausulaRenumeracao, boolean incluidoPorEmenda) {
        return new ItemAnexoParteNormativaResponseDto(id, null, tipo, null, null, null, null, status,
                null, null, null, clausulaEmenda, null, clausulaRenumeracao, incluidoPorEmenda, null, null, List.of());
    }

    private static ItemAnexoParteNormativaResponseDto unico() {
        return par(1, PARAGRAFO_UNICO, ElementoEmendaStatusEnum.INALTERADO, null, null, false);
    }

    private static ItemAnexoParteNormativaResponseDto novoParagrafoPendente() {
        return par(2, PARAGRAFO, ElementoEmendaStatusEnum.INCLUIDO, null, null, true);
    }

    @Test
    void unicoEmVigorQueGanhaUmSegundoParagrafoNoDocumentoPublicadoEhRenumerado() {
        var u = unico();
        assertThat(NumeracaoService.unicoRenumerado(List.of(u, novoParagrafoPendente()), u, true)).isTrue();
    }

    @Test
    void unicoSozinhoNuncaEhRenumerado() {
        var u = unico();
        assertThat(NumeracaoService.unicoRenumerado(List.of(u), u, true)).isFalse();
    }

    @Test
    void emDocumentoNaoPublicadoANumeracaoELivreENadaEhRiscado() {
        var u = unico();
        assertThat(NumeracaoService.unicoRenumerado(List.of(u, novoParagrafoPendente()), u, false)).isFalse();
    }

    @Test
    void unicoIncluidoPorEmendaAindaPendenteNaoEstavaEmVigorEPodeSerRenumeradoLivremente() {
        var u = par(1, PARAGRAFO_UNICO, ElementoEmendaStatusEnum.INCLUIDO, null, null, true);
        assertThat(NumeracaoService.unicoRenumerado(List.of(u, novoParagrafoPendente()), u, true)).isFalse();
    }

    @Test
    void unicoIncluidoEmCicloAnteriorJaPublicadoEstavaEmVigor() {
        var u = par(1, PARAGRAFO_UNICO, ElementoEmendaStatusEnum.INCLUIDO, "(incluído pela Portaria X)", null, true);
        assertThat(NumeracaoService.unicoRenumerado(List.of(u, novoParagrafoPendente()), u, true)).isTrue();
    }

    @Test
    void depoisDePublicadaAAlteracaoOTachadoEPermanenteMesmoComAClausulaCongelada() {
        var u = par(1, PARAGRAFO_UNICO, ElementoEmendaStatusEnum.INALTERADO, null,
                "(redação dada pela Portaria X, publicada no BCA Y)", false);
        var outro = par(2, PARAGRAFO, ElementoEmendaStatusEnum.INCLUIDO, "(incluído pela Portaria X)", null, true);
        assertThat(NumeracaoService.unicoRenumerado(List.of(u, outro), u, true)).isTrue();
    }

    @Test
    void soUmParagrafoUnicoPodeSerRenumeradoNuncaUmNumerado() {
        var p1 = par(1, PARAGRAFO, ElementoEmendaStatusEnum.INALTERADO, null, null, false);
        assertThat(NumeracaoService.unicoRenumerado(List.of(p1, novoParagrafoPendente()), p1, true)).isFalse();
    }

    @Test
    void oUnicoRenumeradoViraParagrafo1eONovoParagrafo2() {
        assertThat(rotulosDeParagrafos(unico(), novoParagrafoPendente())).containsExactly("§ 1º", "§ 2º");
    }

    @Test
    void aClausulaAoVivoUsaOPlaceholderEACongeladaPrevalece() {
        assertThat(NumeracaoService.clausulaRenumeracao(unico()))
                .startsWith("(redação dada pela Portaria DIRAD n° XYZ")
                .contains("publicada no BCA n° ABC");
        var congelada = par(1, PARAGRAFO_UNICO, ElementoEmendaStatusEnum.INALTERADO, null, "(renumerado ... Portaria 7)", false);
        assertThat(NumeracaoService.clausulaRenumeracao(congelada)).isEqualTo("(renumerado ... Portaria 7)");
    }
}
