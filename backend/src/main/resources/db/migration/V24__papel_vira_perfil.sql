-- "Perfil" é o termo do sistema (linguagem ubíqua); "papel" deixa de existir, inclusive no banco:
--   t_papel -> t_perfil (sg_perfil, nm_perfil, ds_perfil) e t_usuario_papel -> t_usuario_perfil (sg_perfil)
-- O dado não muda, só os nomes. (As migrações anteriores não são reescritas: um banco que já as aplicou acusaria checksum diferente.)
ALTER TABLE t_usuario_papel DROP CONSTRAINT fk_usuario_papel_papel;

ALTER TABLE t_papel RENAME TO t_perfil;
ALTER TABLE t_perfil RENAME COLUMN sg_papel TO sg_perfil;
ALTER TABLE t_perfil RENAME COLUMN nm_papel TO nm_perfil;
ALTER TABLE t_perfil RENAME COLUMN ds_papel TO ds_perfil;
ALTER TABLE t_perfil RENAME CONSTRAINT t_papel_pkey TO t_perfil_pkey;
ALTER TABLE t_perfil RENAME CONSTRAINT t_papel_nm_papel_key TO t_perfil_nm_perfil_key;

ALTER TABLE t_usuario_papel RENAME TO t_usuario_perfil;
ALTER TABLE t_usuario_perfil RENAME COLUMN sg_papel TO sg_perfil;
ALTER TABLE t_usuario_perfil RENAME CONSTRAINT t_usuario_papel_pkey TO t_usuario_perfil_pkey;
ALTER TABLE t_usuario_perfil ADD CONSTRAINT fk_usuario_perfil_perfil FOREIGN KEY (sg_perfil) REFERENCES t_perfil (sg_perfil);

-- A ação da auditoria de acessos também mudou de nome (AcaoDeAcessoEnum.ALTEROU_PAPEIS -> ALTEROU_PERFIS).
UPDATE t_log_acesso SET sg_acao = 'ALTEROU_PERFIS' WHERE sg_acao = 'ALTEROU_PAPEIS';

-- Nomes de restrições gerados pelo PostgreSQL que ainda carregavam o termo antigo.
ALTER TABLE t_perfil RENAME CONSTRAINT t_papel_sg_papel_not_null TO t_perfil_sg_perfil_not_null;
ALTER TABLE t_perfil RENAME CONSTRAINT t_papel_nm_papel_not_null TO t_perfil_nm_perfil_not_null;
ALTER TABLE t_perfil RENAME CONSTRAINT t_papel_ds_papel_not_null TO t_perfil_ds_perfil_not_null;
ALTER TABLE t_usuario_perfil RENAME CONSTRAINT t_usuario_papel_usuario_id_not_null TO t_usuario_perfil_usuario_id_not_null;
ALTER TABLE t_usuario_perfil RENAME CONSTRAINT t_usuario_papel_sg_papel_not_null TO t_usuario_perfil_sg_perfil_not_null;
ALTER TABLE t_usuario_perfil RENAME CONSTRAINT t_usuario_papel_usuario_id_fkey TO t_usuario_perfil_usuario_id_fkey;
