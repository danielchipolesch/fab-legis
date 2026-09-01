<div align="center">

# 📘 FAB Legis

**Sistema de elaboração, padronização e gestão de atos normativos do Comando da Aeronáutica**

*Do rascunho à publicação — com numeração automática, editor WYSIWYG e exportação em PDF/DOCX/HTML*

![Java](https://img.shields.io/badge/Java-25-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.0-6DB33F)
![Vue](https://img.shields.io/badge/Vue-3.5-42b883)
![Quasar](https://img.shields.io/badge/Quasar-2.17-1976D2)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791)
![MinIO](https://img.shields.io/badge/MinIO-on--premise-C72E49)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED)

📖 **[Documentação técnica completa](docs/index.md)**

</div>

---

## O que é

O FAB Legis trata o ato normativo (ICA, NSCA, MCA, RCA, DCA, PCA, OCA, RICA, ROCA, TCA, FCA) não como um arquivo de texto, mas como uma estrutura de dados hierárquica: cada capítulo, artigo, parágrafo, inciso e alínea é um nó em uma árvore. Isso permite numeração automática, um editor WYSIWYG com preview em tempo real, exportação fiel em PDF/DOCX/HTML, comparação de versões e um fluxo de aprovação/publicação com papéis, colaboração em tempo real e trilha de auditoria completos.

Para o detalhamento de funcionalidades, arquitetura, modelo de domínio, autenticação, ciclo de vida do documento e API, veja a **[documentação técnica](docs/index.md)** — sobe junto com `docker compose up -d` (ver abaixo), disponível em `http://localhost:8000`.

## Como executar

**Pré-requisito:** Docker e Docker Compose.

```bash
git clone https://github.com/danielchipolesch/fab-legis.git
cd fab-legis
docker compose up -d
```

| Serviço | URL |
|---|---|
| Frontend | http://localhost:5173 |
| Backend (API + Swagger em `/swagger-ui.html`) | http://localhost:8081 |
| Documentação técnica | http://localhost:8000 |
| MinIO (console) | http://localhost:9001 |

Um usuário administrador padrão é criado automaticamente no primeiro boot. Detalhes de credenciais, variáveis de ambiente e execução local (sem Docker) estão na **[documentação técnica → Instalação e Configuração](docs/instalacao.md)**.

## Contribuindo

O desenvolvimento ocorre na branch **`desenvolvimento`**. Ao contribuir:

1. Crie sua branch a partir de `desenvolvimento`;
2. Mantenha o padrão de camadas do backend e a separação de responsabilidades do frontend (ver [Arquitetura](docs/arquitetura.md));
3. Escreva mensagens de commit descritivas em português;
4. Abra um Pull Request para `desenvolvimento`.

---

<div align="center">

**FAB Legis** — padronizando a atividade normativa do Comando da Aeronáutica.

</div>
