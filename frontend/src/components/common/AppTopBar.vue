<template>
  <q-header elevated class="bg-primary text-white">
    <q-toolbar style="height:60px">
      <q-toolbar-title>
        <div class="row items-center no-wrap" style="gap:12px">
          <q-avatar size="36px" color="white" text-color="primary" rounded>
            <q-icon name="mdi-scale-balance" size="22px" />
          </q-avatar>
          <div>
            <div class="text-subtitle1 text-weight-bold text-white" style="line-height:1">FAB Legis</div>
            <div class="text-caption text-white" style="opacity:.75;line-height:1.1">Gestão de Legislação do COMAER</div>
          </div>
        </div>
      </q-toolbar-title>

      <q-btn
        :to="{ name: 'home' }"
        flat
        color="white"
        class="q-mr-xs"
      >
        <q-icon left name="mdi-home-outline" />
        Início
      </q-btn>

      <q-separator vertical class="q-mx-sm" style="opacity:.4" color="white" />

      <q-btn icon="mdi-bell-outline" flat round color="white">
        <q-tooltip anchor="bottom middle" self="top middle">Notificações</q-tooltip>
      </q-btn>

      <q-btn flat color="white" class="q-ml-xs">
        <q-avatar size="30px" color="secondary" text-color="white" class="q-mr-sm">
          <q-icon name="mdi-account" size="18px" />
        </q-avatar>
        <span class="text-caption">Usuário</span>
        <q-icon right name="mdi-chevron-down" />
        <q-menu>
          <q-list dense style="min-width:180px">
            <q-item clickable v-close-popup>
              <q-item-section avatar>
                <q-icon name="mdi-account-outline" />
              </q-item-section>
              <q-item-section>Perfil</q-item-section>
            </q-item>
            <q-item clickable v-close-popup>
              <q-item-section avatar>
                <q-icon name="mdi-cog-outline" />
              </q-item-section>
              <q-item-section>Configurações</q-item-section>
            </q-item>
            <q-separator />
            <q-item clickable v-close-popup>
              <q-item-section avatar>
                <q-icon name="mdi-logout" />
              </q-item-section>
              <q-item-section>Sair</q-item-section>
            </q-item>
          </q-list>
        </q-menu>
      </q-btn>
    </q-toolbar>
  </q-header>
</template>
