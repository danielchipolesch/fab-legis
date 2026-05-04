package br.com.danielchipolesch.infrastructure.enums;

import lombok.Getter;

@Getter
public enum EspecieNormativaEnum {

    DCA("Diretriz do Comando da Aeronáutica", "Espécie destinada, precipuamente, a definir, estabelecer ou orientar em caráter global, setorial ou específico, a Concepção Estratégica do Comando da Aeronáutica nos campos de ação essenciais ao desenvolvimento da Aeronáutica e ao fortalecimento e emprego do Poder Aeroespacial. Reservado apenas ao Comandante da Aeronáutica, através do Decreto-Lei nº 991, de 21 de outubro de 1969, art.63"),
    FCA("Folheto do Comando da Aeronáutica", "Espécie destinada a informar e a noticiar assuntos específicos, de caráter administrativo, técnico, didático, literário, e à publicação de transcrições, reproduções, traduções de livros, artigos, reportagens, discursos, conferências, pronunciamentos, pareceres e relatórios"),
    ICA("Instrução do Comando da Aeronáutica", "Espécie de caráter determinativo e diretivo que é destinada a divulgar regras, preceitos, critérios, recomendações e procedimentos diversos, visando a facilitar, de maneira inequívoca, a aplicação de leis, decretos, portarias e regulamentos"),
    MCA("Manual do Comando da Aeronáutica", "Espécie de caráter diretivo, informativo e/ou didático destinada a regular e a divulgar assuntos relacionados com doutrina, ensino, instrução, técnica, emprego de Unidades, de equipamentos ou de armamentos, podendo, ainda, completar matéria já tratada em outros atos normativos. Os Manuais podem, também, ser usados para compilação de matérias, tais como: os glossários, os dicionários, as relações de abreviaturas, siglas e símbolos"),
    NSCA("Norma de Sistema do Comando da Aeronáutica", "Espécie destinada a disciplinar, tecnicamente, matérias e assuntos ligados à atividade-meio do sistema considerado. As NSCA são elaboradas exclusivamente pelos órgãos centrais dos sistemas, sendo aprovadas por ato do Comandante ou Dirigentes do Órgão Central, de acordo com a competência regimental ou delegada, cujo sistema se situem em suas estruturas de comando, conforme ICA 700-1 (Implantação e Gerenciamento de Sistemas no Comando da Aeronáutica)"),
    OCA("Ordem do Comando da Aeronáutica", "Espécie de caráter determinativo no campo operacional, a qual é destinada a consubstanciar as decisões tomadas em determinado momento para cumprimento de uma missão"),
    PCA("Plano do Comando da Aeronáutica", "Espécie de caráter determinativo que é destinada a consubstanciar as decisões tomadas em um determinado momento e nível hierárquico, a consecução de objetivos finais a serem alcançados no determinado período específico"),
    RCA("Regulamento do Comando da Aeronáutica", "Espécie de caráter determinativo e diretivo que é destinada a dispor sobre a execução de leis ou de decretos e, como tal, destina-se a estabelecer preceitos de administração e demais atividades gerais do Comando da Aeronáutica, tais como: prescrições específicas relativas a recursos humanos, economia, finanças, material, serviços internos, patrimônio e outros assuntos cabíveis de serem regulamentados no seu âmbito. Sua aprovação é exclusiva ao Comandante da Aeronáutica"),
    RICA("Regimento Interno do Comando da Aeronáutica", "Espécie destinada a estabelecer o detalhamento da estrutura da Organização Militar, disciplinando o funcionamento e as competências de seus órgãos constitutivos, em complemento ao respectivo Regulamento de Organização"),
    ROCA("Regulamento de Organização do Comando da Aeronáutica", "Espécie destinada a estabelecer a finalidade, a subordinação, a sede, a estrutura básica e as atribuições gerais de uma Organização Militar. Pode referir-se a uma Organização específica ou a um determinado tipo de Organização. Sua aprovação é exclusiva do Comandante da Aeronáutica"),
    TCA("Tabela do Comando da Aeronáutica", "Espécie destinada a registrar, catalogar, relacionar, listar e divulgar, periódica e detalhadamente, assuntos gerais, tais como: cursos, cálculos, índices, publicações, desdobramentos estruturais, distribuição de material, equipamento, endereços, etc");

    private final String name;
    private final String description;

    EspecieNormativaEnum(String name, String description) {
        this.name = name;
        this.description = description;
    }

}
