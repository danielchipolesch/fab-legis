<template>
  <q-page class="q-pa-xl">

    <q-breadcrumbs active-color="primary" class="q-mb-md" style="font-size:13px">
      <template #separator>
        <q-icon name="mdi-chevron-right" size="16px" color="primary" />
      </template>
      <q-breadcrumbs-el :to="{ name: 'home' }" icon="mdi-home" />
      <q-breadcrumbs-el label="Área de Trabalho" />
    </q-breadcrumbs>

    <div class="row items-center justify-between q-mb-xl">
      <div>
        <h1 class="text-h5 text-weight-bold text-primary q-my-none">Área de Trabalho</h1>
        <p class="text-body2 text-grey-7 q-mb-none">
          Aqui estão reunidos os seus documentos, de todos os módulos: os que estão em andamento, os que aguardam uma ação sua
          e os publicados e revogados. Para trabalhar em um documento, escolha o módulo em <strong>Acesso Rápido</strong>.
        </p>
      </div>

      <!-- Criar só leva ao módulo: quem cria de fato é o botão de criar da tela inicial dele. -->
      <q-btn-dropdown v-if="auth.isEditor" color="primary" unelevated size="lg" icon="mdi-plus" label="Criar" data-testid="criar">
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

    <div class="row q-col-gutter-md">
      <div class="col-12 col-md-4">
        <PainelCard cartao="em_andamento" titulo="Meus documentos em andamento" vazio="Nenhum documento em andamento." @detalhes="abrirDetalhes('em_andamento', $event)" />
      </div>
      <div class="col-12 col-md-4">
        <PainelCard cartao="aguardando" titulo="Aguardando minha ação" vazio="Nenhuma ação pendente" @detalhes="abrirDetalhes('aguardando', $event)" />
      </div>
      <div class="col-12 col-md-4">
        <PainelCard cartao="publicados" titulo="Publicados e revogados" vazio="Nenhum documento publicado." @detalhes="abrirDetalhes('publicados', $event)" />
      </div>
    </div>

    <!-- Acesso Rápido: um botão por módulo -- um módulo novo entra só aqui, sem card novo. -->
    <div class="text-center q-mt-xl q-mb-md">
      <div class="text-h6 text-grey-8">Acesso Rápido</div>
      <div class="text-caption text-grey-7">Selecione uma opção abaixo. Você também poderá acessar essas opções no menu localizado na barra superior.</div>
    </div>
    <q-card flat bordered>
      <q-card-section>
        <div class="row justify-center q-gutter-md">
          <q-btn
            v-for="m in MODULOS"
            :key="m.rota"
            :to="{ name: m.rota }"
            stack
            flat
            no-caps
            class="tile"
            :data-testid="`tile-${m.rota}`"
          >
            <q-icon :name="m.icone" size="44px" color="primary" />
            <div class="text-subtitle2 text-grey-9 q-mt-sm">{{ m.nome }}</div>
            <div class="text-caption text-grey-7">{{ m.subtitulo }}</div>
            <q-tooltip>Acessar o módulo {{ m.nome }}</q-tooltip>
          </q-btn>
        </div>
      </q-card-section>
    </q-card>

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
const detalhesCartao = ref('em_andamento')

function abrirDetalhes(cartao, doc) {
  detalhesCartao.value = cartao
  detalhesDoc.value = doc
  detalhesAberto.value = true
}
</script>

<style scoped>
.tile { width: 200px; min-height: 140px; }
</style>
