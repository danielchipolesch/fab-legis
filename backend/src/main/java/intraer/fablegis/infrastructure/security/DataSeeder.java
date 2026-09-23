package intraer.fablegis.infrastructure.security;

import intraer.fablegis.domain.entities.usuario.OrganizacaoMilitar;
import intraer.fablegis.domain.entities.usuario.PapelEnum;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.domain.util.CpfValidator;
import intraer.fablegis.infrastructure.repositories.OrganizacaoMilitarRepository;
import intraer.fablegis.infrastructure.repositories.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
// Reaproveita a OM "SISTEMA" criada pela migração como OM provisória do
// admin -- ele deveria ser reatribuído à OM real assim que a tela de
// administração de usuários existir (ver fase 02 do design doc).
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private OrganizacaoMilitarRepository omRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Value("${app.admin.cpf:11144477735}")
    private String adminCpf;

    @Value("${app.admin.senha:Admin@123}")
    private String adminSenha;

    @Value("${app.admin.nome:Administrador}")
    private String adminNome;

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

        OrganizacaoMilitar omSistema = omRepository.findBySigla("SISTEMA")
                .orElseThrow(() -> new IllegalStateException(
                        "OM 'SISTEMA' não encontrada -- migração V1__initial.sql não foi aplicada?"));

        Usuario admin = new Usuario();
        admin.setNome(adminNome);
        admin.setCpf(cpf);
        admin.setSenhaHash(passwordEncoder.encode(adminSenha));
        admin.setOm(omSistema);
        admin.setAtivo(true);
        admin.setSistema(false);
        // ADMIN sozinho não basta pra criar documento (ver isEditor em
        // stores/auth.js e o comentário de PapelEnum.ADMIN -- é papel puramente
        // administrativo, sem poder nenhum sobre documentos, de propósito).
        // Sem EDIT aqui, o único usuário que existe logo após a instalação não
        // consegue criar nada até alguém entrar em Usuários e conceder o papel a
        // si mesmo -- então o admin padrão nasce com os dois papéis, e qualquer
        // outro ADMIN criado depois continua sem EDIT implícito nenhum.
        admin.setPapeis(EnumSet.of(PapelEnum.ADMIN, PapelEnum.EDIT));
        usuarioRepository.save(admin);

        log.warn("Usuário administrador padrão criado (CPF {}). Troque a senha padrão assim que possível.", cpf);
    }
}
