package intraer.fablegis.infrastructure.security;

import lombok.RequiredArgsConstructor;
import intraer.fablegis.application.validation.EmailInstitucionalValidator;
import intraer.fablegis.domain.entities.usuario.OrganizacaoMilitar;
import intraer.fablegis.domain.entities.usuario.PapelEnum;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.domain.services.AuditoriaDeAcessoService;
import intraer.fablegis.domain.util.CpfValidator;
import intraer.fablegis.infrastructure.repositories.OrganizacaoMilitarRepository;
import intraer.fablegis.infrastructure.repositories.PostoGraduacaoRepository;
import intraer.fablegis.infrastructure.repositories.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;

// Roda em todo boot, mas só age uma vez: cria o usuário ADMIN padrão apenas
// se nenhum usuário com login (fl_sistema = false) existir ainda -- ou seja,
// só no primeiro deploy. O usuário "sistema" (dono dos documentos
// pré-existentes) já é criado pela própria migração inicial, não aqui.
//
// O administrador padrão nasce na OM do CENDOC (o administrador do sistema, que também cria todas as espécies convencionais,
// ver HabilitacaoPadraoRunner), com posto, nome completo, nome de guerra e e-mail institucional configuráveis (app.admin.*);
// os padrões são os dados do administrador atual do sistema.
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    static final String SIGLA_DA_OM_PADRAO = "CENDOC";

    private final UsuarioRepository usuarioRepository;
    private final OrganizacaoMilitarRepository omRepository;
    private final PostoGraduacaoRepository postoGraduacaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaDeAcessoService auditoriaDeAcesso;

    @Value("${app.admin.cpf:11144477735}")
    private String adminCpf;

    @Value("${app.admin.senha:Admin@123}")
    private String adminSenha;

    @Value("${app.admin.nome:DANIEL ANGELO CHIPOLESCH DE ALMEIDA}")
    private String adminNome;

    @Value("${app.admin.nome-guerra:CHIPOLESCH}")
    private String adminNomeGuerra;

    @Value("${app.admin.email:chipoleschdaca@fab.mil.br}")
    private String adminEmail;

    // Bigrama do posto/graduação (CP = Capitão); vazio = sem posto.
    @Value("${app.admin.posto:CP}")
    private String adminPosto;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean existeUsuarioComLogin = usuarioRepository.findAll().stream().anyMatch(u -> !u.isSistema());
        if (existeUsuarioComLogin) return;

        String cpf = CpfValidator.onlyDigits(adminCpf);
        if (!CpfValidator.isValid(cpf)) {
            log.error("app.admin.cpf ({}) não é um CPF válido -- usuário administrador padrão NÃO foi criado.", adminCpf);
            return;
        }

        OrganizacaoMilitar om = omRepository.findBySigla(SIGLA_DA_OM_PADRAO)
                .orElseThrow(() -> new IllegalStateException(
                        "OM " + SIGLA_DA_OM_PADRAO + " não encontrada -- migração V1__initial.sql não foi aplicada?"));

        Usuario admin = new Usuario();
        admin.setNome(adminNome);
        admin.setNomeGuerra(vazioComoNulo(adminNomeGuerra));
        admin.setCpf(cpf);
        admin.setSenhaHash(passwordEncoder.encode(adminSenha));
        admin.setOm(om);
        admin.setAtivo(true);
        admin.setSistema(false);
        // O e-mail do sistema é sempre institucional (@fab.mil.br): um valor fora do domínio é ignorado, com aviso.
        if (EmailInstitucionalValidator.isValid(adminEmail)) {
            admin.setEmail(adminEmail.trim());
        } else if (!vazio(adminEmail)) {
            log.warn("app.admin.email ({}) não é do domínio @{} -- o administrador padrão foi criado sem e-mail.",
                    adminEmail, EmailInstitucionalValidator.DOMINIO);
        }
        if (!vazio(adminPosto)) {
            postoGraduacaoRepository.findAll().stream()
                    .filter(p -> adminPosto.trim().equalsIgnoreCase(p.getBigrama()))
                    .findFirst()
                    .ifPresentOrElse(admin::setPostoGraduacao,
                            () -> log.warn("app.admin.posto ({}) não existe -- o administrador padrão foi criado sem posto.", adminPosto));
        }
        // ADMIN sozinho não basta pra criar documento (ver isEditor em
        // stores/auth.js e o comentário de PapelEnum.ADM_GER -- é papel puramente
        // administrativo, sem poder nenhum sobre documentos, de propósito).
        // Sem EDIT aqui, o único usuário que existe logo após a instalação não
        // consegue criar nada até alguém entrar em Usuários e conceder o papel a
        // si mesmo -- então o admin padrão nasce com os dois papéis, e qualquer
        // outro ADMIN criado depois continua sem EDIT implícito nenhum.
        admin.setPapeis(EnumSet.of(PapelEnum.ADM_GER, PapelEnum.ELB, PapelEnum.PNL_GER));
        usuarioRepository.save(admin);
        auditoriaDeAcesso.registrarCriacao(admin);

        log.warn("Usuário administrador padrão criado (CPF {}). Troque a senha padrão assim que possível.", cpf);
    }

    private static boolean vazio(String s) {
        return s == null || s.isBlank();
    }

    private static String vazioComoNulo(String s) {
        return vazio(s) ? null : s.trim();
    }
}
