<template>
  <v-app-bar
    color="primary"
    elevation="3"
    height="60"
  >
    <v-app-bar-title>
      <div class="d-flex align-center gap-3">
        <v-avatar size="36" color="white" rounded="sm" class="mr-2">
          <v-icon color="primary" icon="mdi-scale-balance" size="22" />
        </v-avatar>
        <div>
          <div class="text-subtitle-1 font-weight-bold text-white lh-1">FAB Legis</div>
          <div class="text-caption text-white" style="opacity:.75;line-height:1.1">Gestão de Legislação do COMAER</div>
        </div>
      </div>
    </v-app-bar-title>

    <template #append>
      <v-btn
        :to="{ name: 'home' }"
        variant="text"
        color="white"
        prepend-icon="mdi-home-outline"
        class="mr-1"
      >
        Início
      </v-btn>

      <v-divider vertical class="mx-2" style="opacity:.4" />

      <v-btn icon variant="text" color="white">
        <v-icon>mdi-bell-outline</v-icon>
        <v-tooltip activator="parent" location="bottom">Notificações</v-tooltip>
      </v-btn>

      <v-menu>
        <template #activator="{ props: menuProps }">
          <v-btn v-bind="menuProps" variant="text" color="white" class="ml-1">
            <v-avatar size="30" color="secondary" class="mr-2">
              <v-icon size="18">mdi-account</v-icon>
            </v-avatar>
            <span class="text-caption">Usuário</span>
            <v-icon end>mdi-chevron-down</v-icon>
          </v-btn>
        </template>
        <v-list density="compact" min-width="180">
          <v-list-item prepend-icon="mdi-account-outline" title="Perfil" />
          <v-list-item prepend-icon="mdi-cog-outline" title="Configurações" />
          <v-divider />
          <v-list-item prepend-icon="mdi-logout" title="Sair" />
        </v-list>
      </v-menu>
    </template>
  </v-app-bar>
</template>
