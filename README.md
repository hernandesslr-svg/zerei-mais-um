# Zerei Mais Um — Controle Financeiro

**Organize. Planeje. Conquiste.**

Aplicativo Android nativo (Kotlin + Jetpack Compose) de organização financeira pessoal.
Offline-first: na V1 todos os dados ficam no aparelho (Room/SQLite). O app não é banco,
não movimenta dinheiro real, não oferece investimentos e não pede senhas bancárias nem CPF.

## Como abrir e rodar

1. Instale o **Android Studio** (Ladybug 2024.2 ou mais recente) com o Android SDK 35.
2. `File > Open` e selecione esta pasta (`ZereiMaisUm`).
3. Aguarde o Gradle sincronizar (baixa as dependências na primeira vez).
4. Escolha um emulador ou celular com Android 8.0+ e clique em **Run**.

Linha de comando:

```bash
./gradlew assembleDebug          # gera app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # roda os testes de regras de negócio
```

## O que já funciona (V1)

| Módulo | Funcionalidades |
| --- | --- |
| Entrada | Abertura com logo e slogan, cadastro, login, recuperação de senha por código, sair |
| Assistente inicial | 5 etapas (Receitas, Despesas, Dívidas, Patrimônio*, Objetivos), salva a cada item, pode pular e retomar pelo Perfil |
| Painel | Saudação, saldo disponível calculado, receitas e despesas do mês, dívidas pendentes, valores reservados, contas a vencer (marcar como paga), progresso das metas, alertas por regras |
| Movimentações | Adicionar, editar, excluir; categorias; busca; filtro por mês e tipo; recorrência (semanal, mensal, anual); parcelamento; despesas por categoria |
| Dívidas | Cadastro completo, saldo e evolução, juros, parcelas, vencimento, pagamentos (geram despesa vinculada sem duplicar), acordos, quitação com celebração, sugestão de prioridade |
| Caixinhas | 5 iniciais + personalizadas (nome, ícone, cor, meta, data), adicionar, retirar, transferir entre caixinhas, histórico, retirada planejada (1, 3, 6, 7, 12 meses ou data personalizada) |
| Metas | Criar, editar, registrar progresso, quanto guardar por mês até o prazo, celebração ao concluir |
| Perfil | Dados pessoais, foto, nível atual, tema claro/escuro, lembretes de contas a vencer, exportar dados (JSON), excluir conta |
| Notificações | Aviso diário (~9h) de contas vencidas ou que vencem até amanhã; permissão pedida uma vez no Android 13+ |

\* Patrimônio, alertas avançados, relatórios, cartões, moedas ZM, níveis 2–5, medalhas e loja estão nas Fases 2 e 3 do plano.

## Arquitetura

```
app/src/main/java/com/zereimaisum/app/
├── core/         Money, Dates, PasswordHasher, Validators (Kotlin puro)
├── domain/       Regras: painel, recorrência/parcelas, dívidas, caixinhas, projeções (Kotlin puro)
├── data/
│   ├── db/       Entidades, DAOs e AppDatabase (Room)
│   ├── repo/     Repositórios por módulo (Auth, User, Transaction, Debt, Box, Goal)
│   └── session/  DataStore: usuário logado e tema
├── di/           AppContainer (injeção manual)
├── ui/           Tema, componentes reutilizáveis, navegação
└── feature/      auth, onboarding, dashboard, transactions, debts, boxes, profile
```

Regras importantes já implementadas:

- Dinheiro sempre em centavos (`Long`).
- Transferências e pagamentos de fatura nunca contam como despesa (sem contagem em dobro).
- Parcelas somam exatamente o total; recorrência mensal preserva o dia (31/01 → 28/02 → 31/03).
- Todas as consultas filtram por `userId`; excluir a conta apaga tudo em cascata.
- Senhas com PBKDF2-HMAC-SHA256 (120 mil iterações, salt por usuário), bloqueio de 30 s após 5 tentativas.
- O app nunca afirma que o CPF está limpo: fala apenas de "dívidas cadastradas".

## Testes

`app/src/test` cobre o núcleo e o domínio (27 testes): formatação e leitura de valores em R$,
datas, hash de senha, validações, cálculo do painel, parcelas, recorrência, pagamentos de dívida,
priorização, regras das caixinhas e projeções.
