import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'
import globals from 'globals'

// ESLint com o preset "essential" do Vue 3 (eslint-plugin-vue) -- a camada que evita erro real (v-for sem :key, mutar
// prop direto, chave duplicada...), não estilo (indentação de atributo, quebra de linha etc., que é o papel do
// Checkstyle no backend -- combinado com o usuário não instalar por enquanto). Sem regra customizada: primeiro passo
// deliberadamente mínimo (ver CLAUDE.md/docs/instalacao.md).
export default [
  { ignores: ['dist/**', 'node_modules/**'] },

  js.configs.recommended,
  ...pluginVue.configs['flat/essential'],

  {
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: {
        ...globals.browser,
        ...globals.node,
      },
    },
  },

  {
    // App.vue é a raiz da aplicação (nunca usada como <app>): a exceção mais comum a essa regra, feita por nome de
    // arquivo em vez de desligar a regra no projeto inteiro.
    files: ['src/App.vue'],
    rules: {
      'vue/multi-word-component-names': 'off',
    },
  },
]
