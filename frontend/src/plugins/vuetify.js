import 'vuetify/styles'
import '@mdi/font/css/materialdesignicons.css'
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'

const fabTheme = {
  dark: false,
  colors: {
    background: '#F0F0F0',
    surface: '#FFFFFF',
    'surface-card': '#F8F8F8',
    primary: '#1A2E5A',
    'primary-darken-1': '#111E3D',
    secondary: '#2C4A8A',
    accent: '#4A72BD',
    error: '#C62828',
    info: '#1565C0',
    success: '#2E7D32',
    warning: '#E65100',
    // Status
    'status-rascunho': '#616161',
    'status-minuta': '#E65100',
    'status-aprovado': '#2E7D32',
    'status-publicado': '#1565C0',
    'status-arquivado': '#455A64',
    'status-cancelado': '#B71C1C',
    'status-revogado': '#4E342E',
    // UI neutrals
    'border-subtle': '#C5D1DF',
    'text-secondary': '#546E7A',
  },
}

export default createVuetify({
  components,
  directives,
  theme: {
    defaultTheme: 'fabTheme',
    themes: { fabTheme },
  },
  defaults: {
    VBtn: {
      variant: 'flat',
      rounded: 'md',
    },
    VCard: {
      rounded: 'lg',
      elevation: 1,
    },
    VTextField: {
      variant: 'outlined',
      density: 'comfortable',
    },
    VSelect: {
      variant: 'outlined',
      density: 'comfortable',
    },
    VTextarea: {
      variant: 'outlined',
    },
    VChip: {
      rounded: 'md',
    },
  },
})
