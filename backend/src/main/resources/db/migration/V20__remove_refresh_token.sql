-- Migração para Spring Authorization Server (OAuth2 Authorization Code + PKCE):
-- refresh tokens passam a ser gerenciados nativamente pelo próprio Authorization
-- Server (OAuth2AuthorizationService, em memória), não mais por esta tabela
-- própria (ver RefreshTokenService, removido). Ver CLAUDE.md, seção "PLANO
-- TEMPORÁRIO -- migração JWT caseiro -> OAuth2".
DROP TABLE IF EXISTS t_refresh_token;
