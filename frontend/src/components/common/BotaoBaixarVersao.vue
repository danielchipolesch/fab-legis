<template>
  <!-- Duas versões (vigente + em tramitação): o usuário escolhe qual baixar. Uma só: botão simples. -->
  <q-btn-dropdown
    v-if="temVigente && temTramitacao"
    outline
    color="deep-orange-7"
    size="sm"
    :loading="loading"
    :data-testid="testid"
  >
    <template #label>
      <q-icon left :name="icon" />
      {{ label }}
    </template>
    <q-list dense>
      <q-item clickable v-close-popup data-testid="versao-tramitacao" @click="$emit('baixar', 'TRAMITACAO')">
        <q-item-section>Versão em tramitação</q-item-section>
      </q-item>
      <q-item clickable v-close-popup data-testid="versao-vigente" @click="$emit('baixar', 'VIGENTE')">
        <q-item-section>Versão vigente (BCA)</q-item-section>
      </q-item>
    </q-list>
  </q-btn-dropdown>
  <q-btn
    v-else
    outline
    color="deep-orange-7"
    size="sm"
    :loading="loading"
    :data-testid="testid"
    @click="$emit('baixar', temVigente ? 'VIGENTE' : 'TRAMITACAO')"
  >
    <q-icon left :name="icon" />
    {{ label }}
  </q-btn>
</template>

<script setup>
defineProps({
  label: { type: String, required: true },
  icon: { type: String, required: true },
  loading: { type: Boolean, default: false },
  temVigente: { type: Boolean, default: false },
  temTramitacao: { type: Boolean, default: false },
  testid: { type: String, default: undefined },
})
defineEmits(['baixar'])
</script>
