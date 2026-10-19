<!-- papeis: ADM_OM ADM_GER AUD_OM AUD_GER -->
# Administração e auditoria

Para quem tem papel de **Administrador** ou de **Auditor**. O administrador é um papel **puramente administrativo**: ele gerencia acessos, mas não edita, aprova nem publica documentos (para isso a pessoa precisa dos papéis correspondentes).

## Manter Usuários

Menu do usuário › **Manter Usuários**. Aqui o administrador cadastra e mantém as pessoas:

- **Criar** o usuário (CPF, nome, nome de guerra, posto, e-mail `@fab.mil.br`, OM e senha inicial);
- **Editar** os dados, **desativar** e **redefinir a senha**;
- **Conceder ou retirar papéis**, marcando as caixas do formulário.

O que cada administrador alcança:

| | Administrador Geral (CENDOC) | Administrador da OM |
|---|---|---|
| Usuários | De **todas** as OMs | Só os da **própria OM** |
| Papéis que concede | Todos, respeitando a lotação | **Elaborador, Aprovador da OM, Publicador do Boletim Interno, Auditor da OM e Painel Gerencial da OM** |
| Administradores | Mantém os administradores das OMs | **Não altera outro administrador** |

Regras de lotação, que o sistema recusa se não forem cumpridas: **Administrador Geral, Auditor Geral, Publicador do BCA e Painel Gerencial Geral** só existem para quem é do **CENDOC**; o **Publicador do Boletim Interno** existe em **qualquer OM**; **Administrador da OM, Auditor da OM e Painel Gerencial da OM**, para quem é de **outras OMs**. O CENDOC **nunca fica sem Administrador Geral**: o último não pode perder o papel, ser desativado nem mudar de OM. Conceda o papel a outra pessoa antes.

O CPF aparece parcialmente oculto nas telas (`111.***.***-35`).

## Revisão periódica de acessos

Quem tem um papel além do Elaborador precisa ter o acesso **reconfirmado**: a cada **6 meses**, ao **trocar de OM** e quando a conta fica **6 meses sem entrar**. O sistema calcula isso sozinho e mostra na coluna **Acesso** da lista de usuários, com um aviso e o filtro "só os pendentes". Ninguém precisa pedir.

O administrador confere as linhas que cabem a ele, com **Manter acesso** (reconfirma os mesmos papéis) ou **Retirar papéis…**. O Administrador Geral confere os administradores das OMs e o pessoal do CENDOC; o Administrador da OM, os demais usuários da própria OM. Ninguém revisa o próprio acesso. Por ora é só um **aviso**: nada é suspenso sozinho.

## Habilitação de espécies por OM

Para o **Administrador Geral**. Menu do usuário › **Manter Habilitação de Espécies Normativas por OM**. Há uma linha por espécie convencional, com as OMs **habilitadas a criá-la** (só as siglas). Use o seletor da linha para adicionar ou remover OMs; cada mudança é gravada na hora. O filtro no topo mostra o que uma OM específica pode criar. A NPA não aparece aqui: qualquer OM a cria.

## DOM — distintivo da OM

Para o **Administrador Geral**. Menu do usuário › **Manter DOM de OM**. Cada OM tem uma imagem (PNG ou JPEG de até 1 MB) que aparece no alto do cabeçalho da NPA. O distintivo é sempre o **da OM do documento**; uma OM sem distintivo sai sem ele.

## Auditoria

Para o **Auditor da OM** (só a própria OM) e o **Auditor Geral** (todas). Menu do usuário › **Auditoria**, com duas abas:

- **Documentos:** quem visualizou, criou, editou, excluiu, clonou, mudou a situação ou compartilhou cada documento, e quando. O registro sobrevive à exclusão do documento.
- **Acessos:** toda mudança no acesso de um usuário (criação, concessão e retirada de papel, ativação e desativação, troca de OM, redefinição de senha, revisão), com quem fez e o detalhe. Filtre por CPF, ação e período.

O auditor é independente do administrador: quem administra **não** vê a auditoria sem ter o papel de auditor.
