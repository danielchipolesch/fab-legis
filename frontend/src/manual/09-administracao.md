<!-- papeis: ADM_OM ADM_GER AUD_OM AUD_GER -->
# Administração e auditoria

Para quem tem papel de **Administrador** ou de **Auditor**. O administrador é um papel **puramente administrativo**: ele gerencia acessos, mas não edita, aprova nem publica documentos (para isso a pessoa precisa dos papéis correspondentes).

## Manter Usuários

Menu do usuário › **Manter Usuários**. Aqui o administrador cadastra e mantém as pessoas:

- **Criar** o usuário (CPF, nome, nome de guerra, posto, e-mail `@fab.mil.br`, OM e senha inicial);
- **Editar** os dados, **desativar** e **redefinir a senha**;
- **Conceder ou retirar papéis**, marcando as caixas do formulário. O formulário mostra **só os papéis que você pode conceder**: os que não são seus simplesmente não aparecem.

O que cada administrador alcança:

| | Administrador Geral (CENDOC) | Administrador da OM (de qualquer OM, o CENDOC inclusive) |
|---|---|---|
| Usuários | De **todas** as OMs | Só os da **própria OM** (o Administrador da OM do CENDOC vê só o pessoal do CENDOC) |
| Papéis que concede | Todos, respeitando a lotação | **Elaborador, Aprovador da OM, Publicador do Boletim Interno, Auditor da OM e Painel Gerencial da OM** |
| Administradores | Mantém os administradores das OMs (**só ele concede** o Administrador da OM, em qualquer OM) | **Não altera outro administrador** |

Regras de lotação, que o sistema recusa se não forem cumpridas: **Administrador Geral, Auditor Geral, Publicador do BCA e Painel Gerencial Geral** só existem para quem é do **CENDOC**; o **Publicador do Boletim Interno** e o **Painel Gerencial da OM** existem em **qualquer OM**, o CENDOC inclusive (que pode ter o Painel Gerencial da OM junto com o Geral); o **Administrador da OM** existe em **qualquer OM**, o CENDOC inclusive: o CENDOC também é uma unidade, com o seu administrador local ao lado do Administrador Geral; o **Auditor da OM**, só para quem é de **outras OMs** (no CENDOC, use o Auditor Geral). O CENDOC **nunca fica sem Administrador Geral**: o último não pode perder o papel, ser desativado nem mudar de OM. Conceda o papel a outra pessoa antes.

O CPF aparece parcialmente oculto nas telas (`111.***.***-35`).

**A mudança vale na hora para a pessoa.** Ao conceder ou retirar um papel, trocar a OM ou desativar a conta, quem está usando o sistema recebe um aviso e a tela dela se ajusta sozinha, sem sair e entrar de novo: botões, menus e telas que ela ganhou ou perdeu mudam. Conta desativada tem a sessão encerrada.

## Revisão periódica de acessos

Quem tem um papel além do Elaborador precisa ter o acesso **reconfirmado**: a cada **6 meses**, ao **trocar de OM** e quando a conta fica **6 meses sem entrar**. O sistema calcula isso sozinho e mostra na coluna **Acesso** da lista de usuários, com um aviso e o filtro "só os pendentes". Ninguém precisa pedir.

O administrador confere as linhas que cabem a ele **e que estão pendentes**, com **Manter acesso** (reconfirma os mesmos papéis) ou **Retirar papéis…**. Depois que o acesso é revisto, esses botões **somem da linha** e só voltam quando um dos três motivos acontecer de novo. O Administrador Geral confere os administradores das OMs e o pessoal do CENDOC; o Administrador da OM, os demais usuários da própria OM (o do CENDOC, o pessoal do CENDOC). Ninguém revisa o próprio acesso. Por ora é só um **aviso**: nada é suspenso sozinho.

## Habilitação de espécies por OM

Para o **Administrador Geral**. Menu do usuário › **Manter Habilitação de Espécies Normativas por OM**. Há uma linha por espécie convencional, com as OMs **habilitadas a criá-la** (só as siglas). Use o seletor da linha para adicionar ou remover OMs; cada mudança é gravada na hora. O filtro no topo mostra o que uma OM específica pode criar. A NPA não aparece aqui: qualquer OM a cria.

**A mudança vale na hora para quem está usando o sistema.** Quem é da OM habilitada (ou que deixou de ser) vê o botão de criar aparecer ou sumir sozinho, com um aviso na tela, sem precisar recarregar a página.

## DOM — distintivo da OM

Para o **Administrador Geral**. Menu do usuário › **Manter DOM de OM**. Cada OM tem uma imagem (PNG ou JPEG de até 1 MB) que aparece no alto do cabeçalho da NPA. O distintivo é sempre o **da OM do documento**; uma OM sem distintivo sai sem ele.

## Auditoria

Para o **Auditor da OM** (só a própria OM) e o **Auditor Geral** (todas). Menu do usuário › **Auditoria**, com duas abas:

- **Documentos:** quem visualizou, criou, editou, excluiu, clonou, mudou a situação ou compartilhou cada documento, e quando. O registro sobrevive à exclusão do documento.
- **Acessos:** toda mudança no acesso de um usuário (criação, concessão e retirada de papel, ativação e desativação, troca de OM, redefinição de senha, revisão), com quem fez e o detalhe. Filtre por CPF, ação e período.

O auditor é independente do administrador: quem administra **não** vê a auditoria sem ter o papel de auditor.
