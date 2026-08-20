<template>
  <aside class="editor-sidebar">

    <q-linear-progress
      v-if="editorStore.adicionando"
      indeterminate
      color="primary"
      style="height:2px;flex-shrink:0"
    />

    <!-- Informações do documento -->
    <div class="sidebar-doc-header q-px-sm q-pt-md q-pb-sm">
      <div class="text-h6 text-weight-bold text-primary ellipsis" style="line-height:1.3">
        {{ docLabel }}
      </div>
      <div v-if="documento?.titulo" class="text-body2 text-grey-8 q-mt-xxs ellipsis-2-lines">
        {{ documento.titulo }}
      </div>
      <div v-if="documento?.assunto_basico" class="text-caption text-grey-6 q-mt-xxs ellipsis">
        {{ documento.assunto_basico }}
      </div>
    </div>

    <!-- Status -->
    <div class="q-px-sm q-pb-sm">
      <StatusBadge v-if="documento?.status" :status="documento.status" size="sm" />
    </div>

    <q-separator />

    <!-- 5 ícones de ação (funções a definir) -->
    <div class="sidebar-actions row justify-around items-center q-py-xs">
      <q-btn flat round dense size="sm" color="grey-7" icon="mdi-file-document-edit-outline" @click="abrirDialogMeta">
        <q-tooltip anchor="top middle" self="bottom middle">Metadados</q-tooltip>
      </q-btn>
      <q-btn
        flat round dense size="sm" color="grey-7" icon="mdi-source-branch"
        :to="{ name: 'documento-comparar', params: { id: props.documento?.id } }"
      >
        <q-tooltip anchor="top middle" self="bottom middle">Comparar versões</q-tooltip>
      </q-btn>
      <q-btn flat round dense size="sm" color="grey-7" icon="mdi-download-outline">
        <q-tooltip anchor="top middle" self="bottom middle">Exportar</q-tooltip>
      </q-btn>
      <q-btn flat round dense size="sm" color="grey-7" icon="mdi-history">
        <q-tooltip anchor="top middle" self="bottom middle">Histórico</q-tooltip>
      </q-btn>
      <q-btn flat round dense size="sm" color="grey-7" icon="mdi-dots-horizontal">
        <q-tooltip anchor="top middle" self="bottom middle">Mais opções</q-tooltip>
      </q-btn>
    </div>

    <q-separator />

    <!-- Árvore de seções do documento -->
    <div class="sidebar-body">
      <template v-for="secao in secoes" :key="secao.id">

        <div
          class="secao-header q-pa-sm row items-center"
          :class="{ 'secao-header--active': isExpandida(secao.tipo) }"
          @click="toggleSecao(secao.tipo)"
        >
          <q-icon
            :name="isExpandida(secao.tipo) ? 'mdi-chevron-down' : 'mdi-chevron-right'"
            size="16px"
            class="q-mr-xs"
          />
          <q-icon :name="secaoIcon(secao.tipo)" size="14px" :color="secaoIconColor(secao.tipo)" class="q-mr-sm" />
          <span class="text-caption text-weight-bold text-uppercase text-grey-7">
            {{ secao.titulo }}
          </span>
        </div>

        <div v-show="isExpandida(secao.tipo)" class="secao-elementos q-px-xs q-pb-sm">

          <!-- Elementos fixos (parte preliminar) -->
          <template v-if="secao.tipo === 'parte_normativa'">
            <q-tree
              :nodes="normativaElementos"
              node-key="id"
              children-key="filhos"
              v-model:expanded="expandedNorm"
              :selected="selectedId"
              @update:selected="val => val && $emit('select', val)"
              no-selection-unset
              dense
              class="norm-tree"
              no-nodes-label="Sem elementos normativos"
            >
              <template v-slot:default-header="{ node }">
                <div class="row items-center full-width norm-node">
                  <!-- Ícone do tipo -->
                  <q-icon
                    :name="elementIcon(node.tipo)"
                    size="13px"
                    :color="isGroupingType(node.tipo) ? 'primary' : 'blue-grey-6'"
                    class="q-mr-xs"
                    style="flex-shrink:0"
                  />
                  <!-- Rótulo -->
                  <span
                    class="norm-label col ellipsis"
                    :class="{
                      'text-weight-bold text-uppercase': node.tipo === 'capitulo',
                      'text-weight-bold': node.tipo === 'secao_normativa' || node.tipo === 'subsecao_normativa',
                      'text-weight-medium': !isGroupingType(node.tipo),
                    }"
                  >{{ formatLabel(node) }}</span>
                  <!-- Preview de conteúdo -->
                  <span
                    v-if="!isGroupingType(node.tipo) && nodePreview(node)"
                    class="text-caption text-grey-6 q-ml-xs norm-preview"
                  >{{ nodePreview(node) }}</span>
                  <!-- Indicador de vazio -->
                  <q-icon
                    v-if="!isNodeFilled(node)"
                    name="mdi-alert-circle"
                    color="amber-7"
                    size="12px"
                    class="q-ml-xs"
                    style="flex-shrink:0"
                  >
                    <q-tooltip anchor="top middle" self="bottom middle">
                      Vazio — necessário para aprovação
                    </q-tooltip>
                  </q-icon>
                  <!-- Indicador de emenda (quando EM_ALTERACAO). Cadeado = já publicado em
                       ciclo anterior (permanente, ver clausulaEmenda) — distingue de uma
                       emenda feita agora mesmo, ainda pendente nesta alteração. -->
                  <q-chip
                    v-if="isEmAlteracao && node.emendaStatus && node.emendaStatus !== 'INALTERADO'"
                    :color="emendaStatusColor(node.emendaStatus)"
                    text-color="white"
                    dense
                    size="xs"
                    class="q-ml-xs"
                    style="flex-shrink:0;font-size:9px;height:14px;padding:0 4px"
                  >
                    <q-icon v-if="node.clausulaEmenda" name="mdi-lock-outline" size="9px" class="q-mr-xs" />{{ emendaStatusLabel(node.emendaStatus) }}
                    <q-tooltip v-if="node.clausulaEmenda" anchor="top middle" self="bottom middle">
                      Já publicado numa alteração anterior — use os botões para uma nova emenda
                    </q-tooltip>
                  </q-chip>
                  <!-- Ações normais (visíveis ao passar o mouse, ocultas quando EM_ALTERACAO) -->
                  <div v-if="!isEmAlteracao" class="norm-actions row items-center q-ml-xs" style="gap:2px;flex-shrink:0">
                    <q-btn
                      v-if="editorStore.canMoveUp(node.id)"
                      round size="xs" flat dense color="grey" @click.stop="$emit('move-up', node.id)"
                    >
                      <q-icon size="11px" name="mdi-arrow-up" />
                      <q-tooltip anchor="center right" self="center left">Mover acima</q-tooltip>
                    </q-btn>
                    <q-btn
                      v-if="editorStore.canMoveDown(node.id)"
                      round size="xs" flat dense color="grey" @click.stop="$emit('move-down', node.id)"
                    >
                      <q-icon size="11px" name="mdi-arrow-down" />
                      <q-tooltip anchor="center right" self="center left">Mover abaixo</q-tooltip>
                    </q-btn>
                    <q-btn
                      v-if="childOptions(node).length || canDemoteNode(node) || canMoveNode(node)"
                      round size="xs" flat dense color="grey"
                      @click.stop
                    >
                      <q-icon size="11px" name="mdi-plus" />
                      <q-menu>
                        <q-list dense style="min-width:180px">
                          <template v-if="childOptions(node).length">
                            <q-item-label header>Adicionar como filho</q-item-label>
                            <q-item
                              v-for="opt in childOptions(node)"
                              :key="opt.tipo"
                              clickable v-close-popup
                              @click="$emit('add-child', node.id, opt.tipo)"
                            >
                              <q-item-section avatar>
                                <q-icon :name="elementIcon(opt.tipo)" />
                              </q-item-section>
                              <q-item-section>{{ opt.label }}</q-item-section>
                            </q-item>
                            <q-separator />
                          </template>
                          <template v-if="canDemoteNode(node)">
                            <q-item-label header>Reorganizar</q-item-label>
                            <q-item v-if="canPromoteNode(node)" clickable v-close-popup @click="$emit('promote', node.id)">
                              <q-item-section avatar>
                                <q-icon name="mdi-arrow-collapse-up" />
                              </q-item-section>
                              <q-item-section>Promover nível</q-item-section>
                            </q-item>
                            <q-item clickable v-close-popup @click="$emit('demote', node.id)">
                              <q-item-section avatar>
                                <q-icon name="mdi-arrow-expand-down" />
                              </q-item-section>
                              <q-item-section>Rebaixar nível</q-item-section>
                            </q-item>
                            <q-separator />
                          </template>
                          <template v-if="canMoveNode(node)">
                            <q-item-label header>Mover para</q-item-label>
                            <q-item
                              v-for="target in moveTargets(node)"
                              :key="target.id ?? 'root'"
                              clickable v-close-popup
                              @click="$emit('move-to-parent', node.id, target.id)"
                            >
                              <q-item-section avatar>
                                <q-icon :name="target.id === null ? 'mdi-arrow-collapse-up' : 'mdi-folder-move-outline'" />
                              </q-item-section>
                              <q-item-section>{{ target.label }}</q-item-section>
                            </q-item>
                            <q-separator />
                          </template>
                          <q-item clickable v-close-popup class="text-negative" @click="$emit('remove', node.id)">
                            <q-item-section avatar>
                              <q-icon name="mdi-delete-outline" color="negative" />
                            </q-item-section>
                            <q-item-section>Remover</q-item-section>
                          </q-item>
                        </q-list>
                      </q-menu>
                    </q-btn>
                    <q-btn v-else round size="xs" flat dense color="negative" @click.stop="$emit('remove', node.id)">
                      <q-icon size="11px" name="mdi-delete-outline" />
                      <q-tooltip anchor="center right" self="center left">Remover</q-tooltip>
                    </q-btn>
                  </div>
                  <!-- Ações de emenda (visíveis ao passar o mouse, quando EM_ALTERACAO) -->
                  <div v-else class="norm-actions row items-center q-ml-xs" style="gap:2px;flex-shrink:0">
                    <!-- Elemento sem emenda pendente — original INALTERADO, ou ALTERADO/
                         INCLUIDO já publicado num ciclo anterior (permanente): uma nova
                         emenda aqui é tratada como alterar/revogar de novo, exatamente como
                         um original, não como "desfazer"/"excluir" a emenda anterior —
                         é assim que o ciclo se repete também para inclusões -->
                    <template v-if="node.emendaStatus === 'INALTERADO' || ((node.emendaStatus === 'ALTERADO' || node.emendaStatus === 'INCLUIDO') && node.clausulaEmenda)">
                      <q-btn round size="xs" flat dense color="primary" @click.stop="$emit('emenda-alterar', node.id, 'PARTE_NORMATIVA')">
                        <q-icon size="11px" name="mdi-pencil-outline" />
                        <q-tooltip anchor="center right" self="center left">Alterar texto</q-tooltip>
                      </q-btn>
                      <q-btn round size="xs" flat dense color="negative" @click.stop="$emit('emenda-revogar', node.id, 'PARTE_NORMATIVA')">
                        <q-icon size="11px" name="mdi-delete-outline" />
                        <q-tooltip anchor="center right" self="center left">Revogar</q-tooltip>
                      </q-btn>
                    </template>
                    <!-- Emenda pendente nesta alteração (ainda não publicada): pode desfazer -->
                    <template v-else-if="(node.emendaStatus === 'ALTERADO' || node.emendaStatus === 'REVOGADO') && !node.clausulaEmenda">
                      <q-btn round size="xs" flat dense color="warning" @click.stop="$emit('emenda-desfazer', node.id, 'PARTE_NORMATIVA')">
                        <q-icon size="11px" name="mdi-undo-variant" />
                        <q-tooltip anchor="center right" self="center left">Desfazer emenda</q-tooltip>
                      </q-btn>
                    </template>
                    <!-- REVOGADO consolidado numa publicação anterior: permanente, sem ações -->
                    <template v-else-if="node.emendaStatus === 'REVOGADO' && node.clausulaEmenda">
                      <q-icon size="14px" name="mdi-lock-outline" color="grey-6">
                        <q-tooltip anchor="center right" self="center left">Revogado permanentemente por publicação anterior</q-tooltip>
                      </q-icon>
                    </template>
                    <!-- Elemento inserido por emenda ainda não publicado (INCLUIDO pendente):
                         edição livre, reordenação entre incluídos (única exceção à vedação
                         de renumeração) ou exclusão direta -->
                    <template v-else-if="node.emendaStatus === 'INCLUIDO'">
                      <q-btn
                        v-if="editorStore.canReorderIncluido(node.id, -1)"
                        round size="xs" flat dense color="grey" @click.stop="$emit('reordenar-incluido', node.id, 'CIMA')"
                      >
                        <q-icon size="11px" name="mdi-arrow-up" />
                        <q-tooltip anchor="center right" self="center left">Mover acima (entre incluídos)</q-tooltip>
                      </q-btn>
                      <q-btn
                        v-if="editorStore.canReorderIncluido(node.id, 1)"
                        round size="xs" flat dense color="grey" @click.stop="$emit('reordenar-incluido', node.id, 'BAIXO')"
                      >
                        <q-icon size="11px" name="mdi-arrow-down" />
                        <q-tooltip anchor="center right" self="center left">Mover abaixo (entre incluídos)</q-tooltip>
                      </q-btn>
                      <q-btn round size="xs" flat dense color="primary" @click.stop="$emit('emenda-alterar', node.id, 'PARTE_NORMATIVA')">
                        <q-icon size="11px" name="mdi-pencil-outline" />
                        <q-tooltip anchor="center right" self="center left">Editar conteúdo</q-tooltip>
                      </q-btn>
                      <q-btn round size="xs" flat dense color="negative" @click.stop="$emit('emenda-desfazer', node.id, 'PARTE_NORMATIVA')">
                        <q-icon size="11px" name="mdi-trash-can-outline" />
                        <q-tooltip anchor="center right" self="center left">Excluir elemento</q-tooltip>
                      </q-btn>
                    </template>
                  </div>
                </div>
              </template>
            </q-tree>

            <div class="q-mt-sm column q-px-xs" style="gap:4px">
              <div v-if="!isEmAlteracao">
                <q-btn
                  outline
                  color="primary"
                  size="sm"
                  class="full-width"
                  :disable="hasTopLevelArtigos || editorStore.adicionando"
                  :loading="editorStore.adicionando"
                >
                  <q-icon left name="mdi-folder-plus-outline" />
                  Novo Capítulo
                  <q-tooltip v-if="hasTopLevelArtigos" anchor="top middle" self="bottom middle">
                    Remova os artigos soltos antes de adicionar capítulos
                  </q-tooltip>
                  <q-menu v-if="!hasTopLevelArtigos">
                    <q-list dense style="min-width:240px">
                      <q-item-label header>Título do capítulo</q-item-label>
                      <q-item
                        v-for="preset in CAPITULO_PRESETS"
                        :key="preset"
                        clickable
                        v-close-popup
                        :disable="existingCapituloTitulos.has(preset)"
                        @click="$emit('add-capitulo', preset)"
                      >
                        <q-item-section>{{ preset }}</q-item-section>
                      </q-item>
                      <q-separator />
                      <q-item clickable v-close-popup @click="$emit('add-capitulo', '')">
                        <q-item-section avatar>
                          <q-icon name="mdi-pencil-outline" />
                        </q-item-section>
                        <q-item-section>Personalizado (sem título)</q-item-section>
                      </q-item>
                    </q-list>
                  </q-menu>
                </q-btn>
              </div>

              <div v-if="isEmAlteracao">
                <q-btn
                  outline
                  color="green-8"
                  size="sm"
                  class="full-width"
                  @click="$emit('emenda-incluir', 'PARTE_NORMATIVA', normativaElementos)"
                >
                  <q-icon left name="mdi-plus-circle-outline" />
                  Incluir novo elemento
                </q-btn>
              </div>

            </div>
          </template>

          <!-- Seção Anexos -->
          <template v-else-if="secao.tipo === 'anexos'">
            <div v-if="!anexos.length" class="q-px-md q-py-sm text-caption text-grey-6">
              Nenhum anexo vinculado a este documento.
            </div>
            <div v-else class="q-px-xs q-pb-xs">
              <div
                v-for="anexo in anexos"
                :key="anexo.id"
                class="fixed-item row items-center q-px-sm q-py-xs"
              >
                <q-icon name="mdi-image-outline" size="13px" color="teal-6" class="q-mr-sm" />
                <span class="text-caption col ellipsis">
                  ANEXO {{ toRoman(anexo.ordem + 1) }} — {{ anexo.titulo }}
                </span>
                <q-btn
                  flat round dense size="xs" color="negative"
                  icon="mdi-delete-outline"
                  @click.stop="removerAnexo(anexo.id)"
                >
                  <q-tooltip anchor="center right" self="center left">Remover</q-tooltip>
                </q-btn>
              </div>
            </div>
            <div class="q-px-xs q-pb-sm q-mt-xs">
              <q-btn
                outline color="primary" size="sm" class="full-width"
                icon="mdi-plus" label="Adicionar anexo"
                @click="abrirDialogAnexo"
              />
            </div>
          </template>

          <!-- Elementos fixos (parte preliminar) -->
          <template v-else>
            <div
              v-for="el in secao.elementos"
              :key="el.id"
              class="fixed-item row items-center q-px-md q-py-xs"
              :class="{ 'fixed-item--active': selectedId === el.id }"
              @click="$emit('select', el.id)"
            >
              <q-icon :name="elementIcon(el.tipo)" size="13px" color="teal-6" class="q-mr-sm" />
              <span class="text-caption col ellipsis">{{ formatLabel(el) }}</span>
              <q-icon
                v-if="!isElFilled(el)"
                name="mdi-alert-circle"
                color="amber-7"
                size="13px"
                class="q-ml-xs"
              >
                <q-tooltip anchor="top middle" self="bottom middle">
                  Seção vazia — necessária para aprovação
                </q-tooltip>
              </q-icon>
            </div>
          </template>

        </div>

        <q-separator />
      </template>
    </div>
  </aside>

  <!-- Dialog de metadados -->
  <q-dialog v-model="metaDialogOpen" persistent>
    <q-card style="min-width:480px;max-width:560px;width:100%">
      <q-card-section class="row items-center q-pb-none">
        <q-icon name="mdi-file-document-edit-outline" size="20px" color="primary" class="q-mr-sm" />
        <div class="text-h6">Metadados do documento</div>
        <q-space />
        <q-btn icon="mdi-close" flat round dense v-close-popup :disable="metaSalvando" />
      </q-card-section>

      <q-separator class="q-mt-sm" />

      <q-card-section class="q-pa-md scroll" style="max-height:70vh">

        <!-- Identificação -->
        <div class="text-caption text-weight-bold text-grey-6 text-uppercase q-mb-sm">Identificação</div>
        <div class="row q-col-gutter-sm q-mb-md">
          <div class="col-4">
            <q-input :model-value="props.documento?.especie" label="Espécie" outlined dense disable />
          </div>
          <div class="col-4">
            <q-input :model-value="props.documento?.numero_basico" label="Número Básico" outlined dense disable />
          </div>
          <div class="col-4">
            <q-input
              v-model="metaForm.numero_secundario"
              label="Número Secundário"
              outlined dense
              type="number"
            />
          </div>
          <div class="col-12">
            <q-input :model-value="props.documento?.codigo_documento" label="Código do documento" outlined dense disable />
          </div>
        </div>

        <!-- Conteúdo -->
        <div class="text-caption text-weight-bold text-grey-6 text-uppercase q-mb-sm">Conteúdo</div>
        <div class="column q-col-gutter-sm q-mb-md">
          <div>
            <q-input :model-value="props.documento?.assunto_basico" label="Assunto Básico" outlined dense disable />
          </div>
          <div>
            <q-input
              v-model="metaForm.titulo"
              label="Título"
              outlined dense
              autofocus
            />
          </div>
        </div>

        <!-- Situação -->
        <div class="text-caption text-weight-bold text-grey-6 text-uppercase q-mb-sm">Situação</div>
        <div class="row q-col-gutter-sm q-mb-md">
          <div class="col-8">
            <q-input :model-value="props.documento?.status" label="Situação" outlined dense disable />
          </div>
          <div class="col-4">
            <q-input :model-value="props.documento?.qtd_replicas" label="Réplicas" outlined dense disable />
          </div>
        </div>

        <!-- Datas -->
        <div class="text-caption text-weight-bold text-grey-6 text-uppercase q-mb-sm">Datas</div>
        <div class="row q-col-gutter-sm">
          <div class="col-6">
            <q-input :model-value="formatarData(props.documento?.data_criacao)" label="Criação" outlined dense disable />
          </div>
          <div class="col-6">
            <q-input :model-value="formatarData(props.documento?.data_alteracao)" label="Última alteração" outlined dense disable />
          </div>
          <template v-if="props.documento?.data_minuta">
            <div class="col-6">
              <q-input :model-value="formatarData(props.documento?.data_minuta)" label="Minuta" outlined dense disable />
            </div>
          </template>
          <template v-if="props.documento?.data_aprovacao">
            <div class="col-6">
              <q-input :model-value="formatarData(props.documento?.data_aprovacao)" label="Aprovação" outlined dense disable />
            </div>
          </template>
          <template v-if="props.documento?.data_publicacao">
            <div class="col-6">
              <q-input :model-value="formatarData(props.documento?.data_publicacao)" label="Publicação" outlined dense disable />
            </div>
          </template>
          <template v-if="props.documento?.data_arquivamento">
            <div class="col-6">
              <q-input :model-value="formatarData(props.documento?.data_arquivamento)" label="Arquivamento" outlined dense disable />
            </div>
          </template>
          <template v-if="props.documento?.data_revogacao">
            <div class="col-6">
              <q-input :model-value="formatarData(props.documento?.data_revogacao)" label="Revogação" outlined dense disable />
            </div>
          </template>
          <template v-if="props.documento?.data_cancelamento">
            <div class="col-6">
              <q-input :model-value="formatarData(props.documento?.data_cancelamento)" label="Cancelamento" outlined dense disable />
            </div>
          </template>
        </div>

      </q-card-section>

      <q-separator />

      <q-card-actions align="right" class="q-px-md q-py-sm">
        <q-btn flat label="Cancelar" v-close-popup :disable="metaSalvando" />
        <q-btn
          unelevated
          color="primary"
          label="Salvar"
          :loading="metaSalvando"
          @click="salvarMeta"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>

  <!-- Dialog de upload de anexo -->
  <q-dialog v-model="anexoDialogOpen" persistent>
    <q-card style="min-width:400px;max-width:480px;width:100%">
      <q-card-section class="row items-center q-pb-none">
        <q-icon name="mdi-paperclip" size="20px" color="primary" class="q-mr-sm" />
        <div class="text-h6">Adicionar anexo</div>
        <q-space />
        <q-btn icon="mdi-close" flat round dense v-close-popup :disable="anexoUploadando" />
      </q-card-section>

      <q-separator class="q-mt-sm" />

      <q-card-section class="q-pa-md column" style="gap:12px">
        <q-input
          v-model="anexoForm.titulo"
          label="Título do anexo"
          outlined dense autofocus
          :disable="anexoUploadando"
        />
        <q-uploader
          ref="anexoUploaderRef"
          :url="anexoUploadUrl"
          field-name="arquivo"
          :form-fields="anexoFormFields"
          label="Imagem (PNG, JPEG)"
          accept="image/png,image/jpeg,image/jpg"
          :multiple="false"
          :max-files="1"
          :auto-upload="false"
          :disable="!anexoForm.titulo || anexoUploadando"
          flat bordered
          style="max-height:200px;width:100%"
          @added="anexoFileQueued = true"
          @removed="anexoFileQueued = false"
          @uploading="anexoUploadando = true"
          @uploaded="onAnexoUploaded"
          @failed="onAnexoFailed"
        />
      </q-card-section>

      <q-separator />

      <q-card-actions align="right" class="q-px-md q-py-sm">
        <q-btn flat label="Cancelar" v-close-popup :disable="anexoUploadando" />
        <q-btn
          unelevated color="primary" label="Enviar"
          :loading="anexoUploadando"
          :disable="!anexoForm.titulo || !anexoFileQueued"
          @click="iniciarUploadAnexo"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { reactive, ref, computed, watch } from 'vue'
import { useQuasar } from 'quasar'
import StatusBadge from '@/components/common/StatusBadge.vue'
import { formatLabel, elementIcon } from '@/utils/numbering.js'
import { useEditorStore } from '@/stores/editor.js'
import { useDocumentsStore } from '@/stores/documents.js'
import { BASE_URL } from '@/api/client.js'

const $q = useQuasar()
const editorStore = useEditorStore()
const documentsStore = useDocumentsStore()

const props = defineProps({
  documento:      { type: Object, default: null },
  docLabel:       { type: String, default: '' },
  secoes:         { type: Array, default: () => [] },
  selectedId:     { type: String, default: null },
  isEmAlteracao:  { type: Boolean, default: false },
})

const emit = defineEmits([
  'select',
  'move-up', 'move-down',
  'add-child', 'add-artigo', 'add-capitulo',
  'promote', 'demote', 'remove',
  'move-to-parent',
  'reorder-normativa',
  'emenda-alterar', 'emenda-revogar', 'emenda-desfazer', 'emenda-incluir',
  'reordenar-incluido',
])

// ── Dialog de metadados ──────────────────────────────────────────────────────
function formatarData(iso) {
  if (!iso) return '—'
  const [y, m, d] = String(iso).slice(0, 10).split('-')
  const meses = ['jan','fev','mar','abr','mai','jun','jul','ago','set','out','nov','dez']
  return `${+d} ${meses[+m - 1]} ${y}`
}

const metaDialogOpen = ref(false)
const metaSalvando   = ref(false)
const metaForm = reactive({ titulo: '', numero_secundario: '' })

function abrirDialogMeta() {
  metaForm.titulo           = props.documento?.titulo ?? ''
  metaForm.numero_secundario = props.documento?.numero_secundario ?? ''
  metaDialogOpen.value = true
}

async function salvarMeta() {
  if (!props.documento?.id) return
  metaSalvando.value = true
  try {
    await documentsStore.updateMetadados(props.documento.id, {
      titulo:            metaForm.titulo,
      numero_secundario: metaForm.numero_secundario !== '' ? metaForm.numero_secundario : null,
    })
    metaDialogOpen.value = false
    $q.notify({ type: 'positive', message: 'Metadados salvos com sucesso.' })
  } catch (e) {
    $q.notify({ type: 'negative', message: 'Erro ao salvar metadados.' })
  } finally {
    metaSalvando.value = false
  }
}

// ── Capítulos predefinidos ────────────────────────────────────────────────────
const CAPITULO_PRESETS = [
  'DISPOSIÇÕES PRELIMINARES',
  'DISPOSIÇÕES GERAIS',
  'DISPOSIÇÕES FINAIS',
  'DISPOSIÇÕES TRANSITÓRIAS',
]

// ── Tipos de agrupamento / conteúdo ──────────────────────────────────────────
const GROUPING_TIPOS = new Set(['capitulo', 'secao_normativa', 'subsecao_normativa'])
const ARTIGO_TIPOS   = new Set(['artigo', 'paragrafo', 'paragrafo_unico', 'inciso', 'alinea', 'sub_alinea'])

const CHILD_MAP = {
  capitulo:           [
    { tipo: 'secao_normativa', label: 'Seção' },
    { tipo: 'artigo',          label: 'Artigo' },
  ],
  secao_normativa:    [
    { tipo: 'subsecao_normativa', label: 'Subseção' },
    { tipo: 'artigo',             label: 'Artigo' },
  ],
  subsecao_normativa: [{ tipo: 'artigo', label: 'Artigo' }],
  artigo:             [
    { tipo: 'paragrafo_unico', label: 'Parágrafo único' },
    { tipo: 'paragrafo',       label: 'Parágrafo (§)' },
    { tipo: 'inciso',          label: 'Inciso' },
  ],
  paragrafo_unico: [{ tipo: 'inciso', label: 'Inciso' }],
  paragrafo:       [{ tipo: 'inciso', label: 'Inciso' }],
  inciso:          [{ tipo: 'alinea', label: 'Alínea' }],
  alinea:          [{ tipo: 'sub_alinea', label: 'Sub-alínea' }],
}

// ── Helpers p/ q-tree ────────────────────────────────────────────────────────
const isGroupingType = (tipo) => GROUPING_TIPOS.has(tipo)
const canPromoteNode = (node) => ARTIGO_TIPOS.has(node.tipo) && node.tipo !== 'artigo'
const canDemoteNode  = (node) => ARTIGO_TIPOS.has(node.tipo)
const childOptions   = (node) => CHILD_MAP[node.tipo] ?? []

// ── "Mover para" (reparenteamento sem alterar tipo) ─────────────────────────
// A lista de destinos válidos (incluindo "Nível superior", quando aplicável) é
// inteiramente decidida pela store, que é a única fonte de verdade sobre a
// hierarquia — evita que a UI ofereça um destino que viole o agrupamento.
const moveTargets = (node) => editorStore.getMoveTargets(node.id)
const canMoveNode = (node) => moveTargets(node).length > 0

function extractText(conteudo) {
  if (!conteudo) return ''
  try {
    const visit = (node) => {
      if (!node) return ''
      if (node.text) return node.text
      if (node.content) return node.content.map(visit).join('')
      return ''
    }
    return visit(JSON.parse(conteudo)).trim()
  } catch { return '' }
}

const isNodeFilled = (node) => isGroupingType(node?.tipo)
  ? (node?.titulo ?? '').trim().length > 0
  : extractText(node?.conteudo).length > 0

const nodePreview = (node) => {
  const text = extractText(node?.conteudo)
  return text.length > 28 ? text.slice(0, 28) + '…' : text
}

// ── Estado das seções colapsadas ─────────────────────────────────────────────
const secaoExpandida = reactive({
  parte_preliminar: true,
  parte_normativa:  true,
  anexos:           false,
})

function toggleSecao(tipo) {
  secaoExpandida[tipo] = !secaoExpandida[tipo]
}

function isExpandida(tipo) {
  return secaoExpandida[tipo] !== false
}

// ── Elementos normativos (lista reativa) ─────────────────────────────────────
const normativaElementos = computed(() => {
  const s = props.secoes.find(s => s.tipo === 'parte_normativa')
  return s?.elementos ?? []
})

// ── Nós expandidos no q-tree (começa com tudo aberto) ───────────────────────
const expandedNorm = ref([])

function collectIds(elements) {
  const ids = []
  for (const el of elements ?? []) {
    ids.push(el.id)
    if (el.filhos?.length) ids.push(...collectIds(el.filhos))
  }
  return ids
}

watch(normativaElementos, (els) => {
  const all = new Set([...expandedNorm.value, ...collectIds(els)])
  expandedNorm.value = [...all]
}, { immediate: true })

// ── Computeds para os botões de adicionar ────────────────────────────────────
const hasCapitulos       = computed(() => normativaElementos.value.some(e => e.tipo === 'capitulo'))
const hasTopLevelArtigos = computed(() => normativaElementos.value.some(e => e.tipo === 'artigo'))

const existingCapituloTitulos = computed(() =>
  new Set(
    normativaElementos.value
      .filter(e => e.tipo === 'capitulo' && e.titulo)
      .map(e => e.titulo.toUpperCase())
  )
)

// ── Anexos ───────────────────────────────────────────────────────────────────
const anexos = computed(() => documentsStore.anexosPorDocumento[String(props.documento?.id)] ?? [])

const anexoDialogOpen = ref(false)
const anexoUploadando = ref(false)
const anexoFileQueued = ref(false)
const anexoForm = reactive({ titulo: '' })
const anexoUploaderRef = ref(null)

const anexoUploadUrl = computed(() => `${BASE_URL}/documentos/${props.documento?.id}/anexos`)
const anexoFormFields = computed(() => [{ name: 'titulo', value: anexoForm.titulo }])

function toRoman(n) {
  if (n <= 0) return ''
  const vals = [1000,900,500,400,100,90,50,40,10,9,5,4,1]
  const syms = ['M','CM','D','CD','C','XC','L','XL','X','IX','V','IV','I']
  let result = ''
  for (let i = 0; i < vals.length; i++)
    while (n >= vals[i]) { result += syms[i]; n -= vals[i] }
  return result
}

function abrirDialogAnexo() {
  anexoForm.titulo = ''
  anexoFileQueued.value = false
  anexoUploadando.value = false
  anexoUploaderRef.value?.reset()
  anexoDialogOpen.value = true
}

function iniciarUploadAnexo() {
  anexoUploaderRef.value?.upload()
}

async function onAnexoUploaded() {
  anexoUploadando.value = false
  anexoDialogOpen.value = false
  if (props.documento?.id) await documentsStore.fetchAnexos(props.documento.id)
  $q.notify({ type: 'positive', message: 'Anexo adicionado com sucesso.' })
}

function onAnexoFailed() {
  anexoUploadando.value = false
  $q.notify({ type: 'negative', message: 'Erro ao enviar anexo.' })
}

function removerAnexo(anexoId) {
  if (!props.documento?.id) return
  $q.dialog({
    title: 'Confirmar exclusão',
    message: 'Deseja realmente excluir este anexo? Esta ação não pode ser desfeita.',
    cancel: { flat: true, label: 'Cancelar' },
    ok: { unelevated: true, color: 'negative', label: 'Excluir' },
    persistent: true,
  }).onOk(async () => {
    try {
      await documentsStore.removeAnexo(props.documento.id, anexoId)
      $q.notify({ type: 'positive', message: 'Anexo removido.' })
    } catch (e) {
      $q.notify({ type: 'negative', message: 'Erro ao remover anexo.' })
    }
  })
}

watch(() => props.documento?.id, (id) => {
  if (id) documentsStore.fetchAnexos(id)
}, { immediate: true })

// ── Helpers p/ itens fixos ───────────────────────────────────────────────────
const isElFilled = (el) => extractText(el?.conteudo).length > 0

// ── Helpers de emenda ────────────────────────────────────────────────────────
function emendaStatusColor(status) {
  return { ALTERADO: 'blue-7', REVOGADO: 'negative', INCLUIDO: 'green-8' }[status] ?? 'grey'
}
function emendaStatusLabel(status) {
  return { ALTERADO: 'ALT', REVOGADO: 'REV', INCLUIDO: 'INC' }[status] ?? status
}

function secaoIcon(tipo) {
  const m = {
    parte_preliminar: 'mdi-text-box-outline',
    parte_normativa:  'mdi-format-list-numbered',
    anexos:           'mdi-paperclip',
  }
  return m[tipo] ?? 'mdi-folder-outline'
}

function secaoIconColor(tipo) {
  const m = {
    parte_preliminar: 'teal-6',
    parte_normativa:  'primary',
    anexos:           'teal-6',
  }
  return m[tipo] ?? 'grey-6'
}
</script>

<style scoped>
.editor-sidebar {
  flex-shrink: 0;
  width: 290px;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--color-surface);
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.12);
  z-index: 1;
  overflow: hidden;
}

/* Cabeçalho do documento */
.sidebar-doc-header {
  flex-shrink: 0;
}

/* Ícones de ação */
.sidebar-actions {
  flex-shrink: 0;
}
.sidebar-actions .q-btn:hover {
  color: var(--q-primary) !important;
  background: rgba(74, 111, 165, 0.08);
}

/* Árvore de seções */
.sidebar-body {
  overflow-y: auto;
  flex: 1 1 auto;
}

.secao-header {
  cursor: pointer;
  background: var(--color-surface);
}
.secao-header:hover {
  background: rgba(74, 111, 165, 0.08);
}
.secao-header--active {
  background: rgba(74, 111, 165, 0.14);
}

/* Itens fixos (parte preliminar / parte final) */
.fixed-item {
  cursor: pointer;
  border-radius: 6px;
  transition: background 0.15s;
  min-height: 26px;
}
.fixed-item:hover {
  background: rgba(74, 111, 165, 0.08);
}
.fixed-item--active {
  background: rgba(74, 111, 165, 0.16);
}

/* ── Q-Tree parte normativa ──────────────────────────────────────────────── */
.norm-tree {
  padding: 0 !important;
}

/* Cabeçalho de cada nó */
.norm-tree :deep(.q-tree__node-header) {
  padding: 2px 4px 2px 2px;
  border-radius: 6px;
  min-height: 28px;
  align-items: center;
}
.norm-tree :deep(.q-tree__node-header:hover) {
  background: rgba(74, 111, 165, 0.08);
}

/* Nó selecionado */
.norm-tree :deep(.q-tree__node--selected > .q-tree__node-header) {
  background: rgba(74, 111, 165, 0.16) !important;
}

/* Seta de expansão */
.norm-tree :deep(.q-tree__arrow) {
  color: var(--q-primary);
  opacity: 0.65;
  font-size: 14px;
}

/* Rótulo do nó */
.norm-label {
  font-size: 0.78rem;
  line-height: 1.25;
}

/* Preview de conteúdo (max ~60px) */
.norm-preview {
  max-width: 56px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex-shrink: 0;
  font-size: 0.7rem;
}

/* Ações aparecem ao passar o mouse */
.norm-actions {
  opacity: 0;
  transition: opacity 0.15s;
}
.norm-tree :deep(.q-tree__node-header:hover .norm-actions) {
  opacity: 1;
}
</style>
