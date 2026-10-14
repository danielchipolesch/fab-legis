-- O termo do sistema é "papel": as descrições gravadas em t_papel pelas migrações V22 e V23 dizem "(perfil exclusivo do CENDOC)"
-- e voltam a dizer "(papel exclusivo do CENDOC)", como o enum PapelEnum (PapelEnumIntegracaoTest confere).
UPDATE t_papel SET ds_papel = replace(ds_papel, '(perfil exclusivo do CENDOC)', '(papel exclusivo do CENDOC)');
