package br.com.danielchipolesch.infrastructure.security;

import br.com.danielchipolesch.domain.entities.usuario.OrganizacaoMilitar;
import br.com.danielchipolesch.domain.entities.usuario.PapelEnum;
import br.com.danielchipolesch.domain.entities.usuario.Usuario;
import br.com.danielchipolesch.domain.util.CpfValidator;
import br.com.danielchipolesch.infrastructure.repositories.OrganizacaoMilitarRepository;
import br.com.danielchipolesch.infrastructure.repositories.UsuarioRepository;
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
// pré-existentes) já é criado pela própria migração V9, não aqui.
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
                        "OM 'SISTEMA' não encontrada -- migração V9 não foi aplicada?"));

        Usuario admin = new Usuario();
        admin.setNome(adminNome);
        admin.setCpf(cpf);
        admin.setSenhaHash(passwordEncoder.encode(adminSenha));
        admin.setOm(omSistema);
        admin.setAtivo(true);
        admin.setSistema(false);
        admin.setPapeis(EnumSet.of(PapelEnum.ADMIN, PapelEnum.APROVADOR));
        usuarioRepository.save(admin);

        log.warn("Usuário administrador padrão criado (CPF {}). Troque a senha padrão assim que possível.", cpf);
    }
}
