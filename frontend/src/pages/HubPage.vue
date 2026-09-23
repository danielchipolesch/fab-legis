<template>
  <q-page class="q-pa-xl">

    <q-breadcrumbs active-color="primary" class="q-mb-md" style="font-size:13px">
      <template #separator>
        <q-icon name="mdi-chevron-right" size="16px" color="primary" />
      </template>
      <q-breadcrumbs-el :to="{ name: 'home' }" icon="mdi-home" />
      <q-breadcrumbs-el label="Área de Trabalho" />
    </q-breadcrumbs>

    <!-- O texto ocupa o que sobra (col) e o botão fica no canto superior direito (col-auto): sem isso, com o texto longo a linha
         quebrava e o Criar caía para a esquerda, embaixo do texto. -->
    <div class="row items-start no-wrap q-mb-xl" style="gap: 16px">
      <div class="col">
        <h1 class="text-h5 text-weight-bold text-primary q-my-none">Área de Trabalho</h1>
        <!-- Adaptado da Área de Trabalho do Compras.gov.br: o que a tela é, o que há em cada quadro e para onde ir. -->
        <div class="text-body2 text-grey-7" data-testid="apresentacao">
          <p class="q-mb-none">Esta é a sua área de trabalho do <strong>FAB Legis</strong>.</p>
          <p class="q-mb-none">
            Aqui estão reunidas as suas tarefas diárias nos quadros abaixo, divididos em quatro cards -
            <strong>Meus Documentos em Tramitação</strong>, <strong>Aguardando Minha Ação</strong>,
            <strong>Documentos em Tramitação nas OMs</strong> e <strong>Publicados e Revogados</strong>. É possível acompanhar os
            seus documentos e as ações que dependem de você, ver o que as outras OMs estão tramitando e consultar tudo o que
            foi publicado ou revogado.
          </p>
          <p class="q-mb-none">
            Também é possível abrir os detalhes de cada documento pelo botão de três pontos (⋮), bem como acessar os módulos de
            <strong>Espécies Convencionais</strong> e <strong>NPA</strong> pelo Acesso Rápido, abaixo, ou pelo botão
            <strong>Criar</strong>.
          </p>
        </div>
      </div>

      <!-- Criar só leva ao módulo: quem cria de fato é o botão de criar da tela inicial dele. -->
      <q-btn-dropdown v-if="auth.isEditor" class="col-auto" color="primary" unelevated size="lg" icon="mdi-plus" label="Criar" data-testid="criar">
        <q-list style="min-width:240px">
          <q-item v-for="m in MODULOS" :key="m.rota" clickable v-close-popup :to="{ name: m.rota }" :data-testid="`criar-${m.rota}`">
            <q-item-section avatar><q-icon :name="m.icone" color="primary" /></q-item-section>
            <q-item-section>
              <q-item-label>{{ m.rotuloDoCriar }}</q-item-label>
              <q-item-label caption>Abre o módulo {{ m.nome }}</q-item-label>
            </q-item-section>
          </q-item>
        </q-list>
      </q-btn-dropdown>
    </div>

    <!-- Quatro cards na mesma linha (a partir de md; abaixo disso, um embaixo do outro). Cada documento fica num só lugar:
         o que a pessoa tramita, o que depende dela, o que os outros tramitam e o que está publicado/revogado sem tramitação. -->
    <div class="row q-col-gutter-md">
      <div class="col-12 col-md-3">
        <PainelCard
          cartao="minhas_em_tramitacao"
          titulo="Meus Documentos em Tramitação"
          vazio="Nenhum documento em tramitação."
          @detalhes="abrirDetalhes('minhas_em_tramitacao', $event)"
        />
      </div>
      <div class="col-12 col-md-3">
        <PainelCard
          cartao="aguardando"
          titulo="Aguardando Minha Ação"
          vazio="Nenhuma ação pendente"
          @detalhes="abrirDetalhes('aguardando', $event)"
        />
      </div>
      <div class="col-12 col-md-3">
        <PainelCard
          cartao="em_tramitacao_de_outros"
          titulo="Documentos em Tramitação nas OMs"
          vazio="Nenhum documento em tramitação."
          ajuda="Documentos em tramitação, de qualquer OM, dos quais você não é autor nem coautor (os seus estão em “Meus Documentos em Tramitação”). Aqui você só pode visualizá-los."
          @detalhes="abrirDetalhes('em_tramitacao_de_outros', $event)"
        />
      </div>
      <div class="col-12 col-md-3">
        <PainelCard
          cartao="publicados"
          titulo="Publicados e Revogados"
          vazio="Nenhum documento publicado."
          ajuda="Documentos publicados (BCA ou Boletim Interno) ou revogados, de qualquer OM, sem tramitação em curso. Se uma alteração ou revogação estiver em tramitação, o documento aparece no card de tramitação (o seu ou o das OMs) até ela terminar."
          @detalhes="abrirDetalhes('publicados', $event)"
        />
      </div>
    </div>

    <!-- Acesso Rápido: um botão por módulo -- um módulo novo entra só aqui, sem card novo. -->
    <div class="text-center q-mt-xl q-mb-md">
      <div class="text-h6 text-grey-8">Acesso Rápido</div>
      <div class="text-caption text-grey-7">Selecione uma opção abaixo. Você também poderá acessar essas opções pelo botão Criar.</div>
    </div>
    <!-- Cada módulo é o seu próprio card quadrado, centralizado: ocupam só o espaço de que precisam (quebram de linha se um dia
         entrarem mais). O q-btn ocupa o card inteiro, para o card todo ser um link (teclado e foco incluídos). -->
    <div class="row justify-center q-gutter-md">
      <q-card v-for="m in MODULOS" :key="m.rota" flat bordered class="tile-modulo">
        <q-btn
          :to="{ name: m.rota }"
          stack
          flat
          no-caps
          class="tile-botao full-width full-height"
          :data-testid="`tile-${m.rota}`"
        >
          <q-icon :name="m.icone" size="48px" color="primary" />
          <div class="text-subtitle1 text-weight-medium text-grey-9 q-mt-sm">{{ m.nome }}</div>
          <div class="text-caption text-grey-7">{{ m.subtitulo }}</div>
          <q-tooltip>Acessar o módulo {{ m.nome }}</q-tooltip>
        </q-btn>
      </q-card>
    </div>

    <DetalhesDoDocumentoDialog v-model="detalhesAberto" :doc="detalhesDoc" :cartao="detalhesCartao" />
  </q-page>
</template>

<script setup>
import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth.js'
import PainelCard from '@/components/hub/PainelCard.vue'
import DetalhesDoDocumentoDialog from '@/components/hub/DetalhesDoDocumentoDialog.vue'
import { MODULOS } from '@/perfis/index.js'

// O hub, a tela inicial do sistema: três cards do trabalho da pessoa (de qualquer módulo) e, embaixo, o acesso a cada
// módulo. Nada aqui altera um documento -- as ações acontecem nos módulos, nas telas de Revisão/Publicação ou no editor.
const auth = useAuthStore()

const detalhesAberto = ref(false)
const detalhesDoc = ref(null)
const detalhesCartao = ref('minhas_em_tramitacao')

function abrirDetalhes(cartao, doc) {
  detalhesCartao.value = cartao
  detalhesDoc.value = doc
  detalhesAberto.value = true
}
</script>

<style scoped>
/* O hover e o foco do botão são retangulares: o card corta o que passa do canto arredondado dele, e o botão herda o mesmo raio. */
.tile-modulo { width: 220px; height: 220px; overflow: hidden; }
.tile-botao { border-radius: inherit; }
</style>
