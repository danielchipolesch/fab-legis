package intraer.fablegis.domain.regras.comunicacaooficialpadronizada;

import intraer.fablegis.domain.handlers.exceptions.InvalidInputException;

import java.util.Set;

// O DOM (distintivo da OM) que a NPA mostra no cabeçalho: uma imagem PNG ou JPEG de até 1 MB. Só esses formatos, porque
// são os que o gerador de PDF (Apache FOP) e o HTML exportado desenham sem conversão.
public final class DistintivoDaOm {

    public static final long TAMANHO_MAXIMO_EM_BYTES = 1024 * 1024;
    private static final Set<String> TIPOS_ACEITOS = Set.of("image/png", "image/jpeg");

    private DistintivoDaOm() {}

    public static void validar(String tipoDoArquivo, long tamanhoEmBytes) {
        if (tipoDoArquivo == null || !TIPOS_ACEITOS.contains(tipoDoArquivo.toLowerCase())) {
            throw new InvalidInputException("O distintivo da OM deve ser uma imagem PNG ou JPEG.");
        }
        if (tamanhoEmBytes <= 0) {
            throw new InvalidInputException("O arquivo do distintivo está vazio.");
        }
        if (tamanhoEmBytes > TAMANHO_MAXIMO_EM_BYTES) {
            throw new InvalidInputException("O distintivo da OM deve ter no máximo 1 MB.");
        }
    }
}
