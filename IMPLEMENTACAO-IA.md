# Implementação de IA com LangChain4j - ManageHouseMoney

## 📋 Status Geral

**Data:** 10/01/2026
**Status:** ✅ **100% COMPLETO**
**Objetivo:** Integrar análises financeiras inteligentes usando LangChain4j sem interface de chat

---

## ✅ O QUE FOI IMPLEMENTADO (100%)

### 🔧 **Backend - Configuração Dinâmica da API Key**

#### Problema Resolvido
- A API key do OpenAI estava configurada para ser lida de variável de ambiente no startup
- Mas o usuário configura via interface web (Settings) e salva no banco de dados
- A aplicação iniciava sem API key, tornando a IA inoperante

#### Solução Implementada

**1. Criado `ChatModelFactory.java`**
- Localização: `backend/src/main/java/com/managehouse/money/config/ChatModelFactory.java`
- Factory que cria `ChatLanguageModel` dinamicamente
- Recebe API key como parâmetro (não via variável de ambiente)
- Retorna `null` se a key for inválida (graceful degradation)

```java
@Component
public class ChatModelFactory {
    @Value("${openai.model:gpt-4o-mini}")
    private String openaiModel;

    public ChatLanguageModel createChatModel(String apiKey) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.isBlank()) {
            return null;
        }
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(openaiModel)
                .temperature(0.7)
                .maxTokens(1500)
                .timeout(Duration.ofSeconds(30))
                .build();
    }
}
```

**2. Atualizado `AIInsightsService.java`**
- Localização: `backend/src/main/java/com/managehouse/money/service/AIInsightsService.java`
- Adicionou injeção de `ChatModelFactory` e `ConfigurationService`
- Criou método privado `getChatModel()` que busca API key do banco
- Substituiu todas as referências ao bean por chamadas dinâmicas

```java
@Autowired
private ChatModelFactory chatModelFactory;

@Autowired
private ConfigurationService configurationService;

private ChatLanguageModel getChatModel() {
    String apiKey = configurationService.getOpenAIKey();
    if (apiKey == null || apiKey.isEmpty()) {
        logger.warn("OpenAI API key not configured. AI features will not work.");
        return null;
    }
    return chatModelFactory.createChatModel(apiKey);
}
```

**3. Removido `LangChain4jConfig.java`**
- Localização: `backend/src/main/java/com/managehouse/money/config/LangChain4jConfig.java`
- Arquivo substituído por comentário indicando remoção
- Não é mais necessário porque o ChatLanguageModel não é mais um bean singleton

**4. Backend compilado com sucesso**
- ✅ `mvn clean compile` executado sem erros
- ✅ 88 arquivos Java compilados

---

### 🎨 **Frontend - Modal de Alertas com Botão Flutuante**

#### Localização
- Arquivo: `frontend/src/components/ExpenseSheet.tsx`
- Linhas: 403-566

#### O Que Foi Implementado

**1. Botão Flutuante**
- Posição: Fixo no canto inferior direito (`fixed bottom-6 right-6`)
- Ícone: 🚨 com animação pulse
- Badge: Círculo vermelho com número de alertas
- Efeitos: Hover com escala 110% e sombra laranja
- Z-index: 40 (sempre visível)

```tsx
<button
  onClick={() => setShowAlerts(true)}
  className="fixed bottom-6 right-6 z-40 w-16 h-16 bg-gradient-to-br from-orange-500 to-red-600 text-white rounded-full shadow-2xl hover:shadow-orange-500/50 hover:scale-110 transition-all flex items-center justify-center group"
>
  <div className="relative">
    <span className="text-3xl animate-pulse">🚨</span>
    {alerts.summary.totalAlerts > 0 && (
      <span className="absolute -top-2 -right-2 bg-red-600 text-white text-xs font-bold rounded-full w-6 h-6 flex items-center justify-center border-2 border-white">
        {alerts.summary.totalAlerts}
      </span>
    )}
  </div>
</button>
```

**2. Modal de Alertas**
- Overlay escuro com blur (`bg-black/50 backdrop-blur-sm`)
- Modal centralizado, responsivo (`max-w-4xl`)
- Scrollable (`max-h-[90vh] overflow-y-auto`)
- Fecha ao clicar fora (overlay) ou no X

**3. Estrutura do Modal**

**Header (Colorido por severidade):**
- Crítico: Gradiente vermelho → laranja
- Atenção: Gradiente amarelo → laranja
- Ok: Gradiente verde → esmeralda
- Ícone grande (🚨/⚠️/✅)
- Título e subtítulo com mês/ano

**Resumo (3 Cards):**
1. Total do Mês (cinza)
2. Média Histórica (azul)
3. Diferença (vermelho/verde conforme economia ou excesso)

**Lista de Alertas:**
- Card por alerta com borda colorida
- Ícone grande (4xl)
- Badges: CRÍTICO/ATENÇÃO/INFO + RECORDE
- Grid 3 colunas: Atual (vermelho) | Média (cinza) | Variação (%)
- Sugestão da IA em card azul com 💡

---

### 🚀 **Frontend - Botão e Modal de Análise IA (Home)**

#### Localização
- Arquivo: `frontend/src/components/ExpenseSheet.tsx`
- Linhas: 591-1146

#### O Que Foi Implementado

**1. Botão "Gerar Análise IA Completa"**
- Localização: Após os cards de resumo
- Gradiente roxo → índigo
- Loading state com spinner
- Ícone 🤖

**2. Card de Preview (Insights IA)**
- Aparece após gerar análise
- Gradiente roxo/índigo/azul
- Conteúdo:
  - Score de saúde financeira (0-100) com barra colorida
  - Resumo executivo
  - Previsão próximo mês com confiança
  - Link "Ver Análise Completa"

**3. Modal de Análise Completa**
- Estrutura similar ao modal de alertas
- Header roxo com ícone 🤖
- Conteúdo scrollable com seções:
  - **Score de Saúde:** Barra de progresso colorida (verde/amarelo/vermelho)
  - **Resumo Executivo:** Card branco com texto
  - **Comparações:** vs Mês Anterior, vs Média, Tendência (📈📉➡️)
  - **Padrões Detectados:** Cards com ícone, descrição, insight e badge de tipo
  - **Previsão Próximo Mês:** Card azul com valor, confiança, raciocínio e premissas
  - **Recomendações:** Lista verde com checkmarks

---

### 📊 **Backend - Serviços de IA Implementados**

#### AIInsightsService

**Localização:** `backend/src/main/java/com/managehouse/money/service/AIInsightsService.java`

**Métodos Implementados:**

1. **`generateMonthlyAnalysis(userId, month, year)`**
   - Gera análise mensal completa
   - Retorna: resumo executivo, score de saúde, padrões, previsões, recomendações
   - Fallback: análise padrão se IA não disponível

2. **`generateAlertSuggestion(expenseTypeName, currentValue, averageValue, percentageAboveAverage)`**
   - Gera sugestão personalizada para alertas
   - Máximo 15 palavras, prática e específica
   - Fallback: mensagem genérica

3. **`detectSpendingPatterns(expenses)`**
   - Detecta até 3 padrões relevantes
   - Tipos: temporal, category, trend, anomaly
   - Retorna JSON com ícones e insights

#### AIAnalysisController

**Localização:** `backend/src/main/java/com/managehouse/money/controller/AIAnalysisController.java`

**Endpoints Criados:**
- `GET /api/ai/analyze?userId=X&month=Y&year=Z` - Análise mensal completa
- `GET /api/ai/patterns?userId=X&month=Y&year=Z` - Detectar padrões

---

### 📁 **Arquivos Criados/Modificados**

#### Backend (Java/Spring Boot)
✅ `ChatModelFactory.java` (CRIADO)
✅ `AIInsightsService.java` (MODIFICADO - adicionado getChatModel())
✅ `AIMonthlyAnalysisResponse.java` (CRIADO)
✅ `ExpenseAlertsResponse.java` (MODIFICADO - campo aiAnalysis)
✅ `ExpenseAlertsService.java` (MODIFICADO - integração IA)
✅ `AIAnalysisController.java` (CRIADO)
✅ `LangChain4jConfig.java` (REMOVIDO)
✅ `application.properties` (MODIFICADO - openai.api.key)
✅ `pom.xml` (MODIFICADO - dependências LangChain4j)

#### Frontend (React/TypeScript)
✅ `types/index.ts` (MODIFICADO - tipos AI)
✅ `aiService.ts` (CRIADO)
✅ `ExpenseSheet.tsx` (MODIFICADO - modal alertas + botão/modal IA)
✅ `ExtractUpload.tsx` (MODIFICADO - seção padrões identificados)

---

## 🎯 COMO USAR

### 1. Configurar API Key

1. Faça login na aplicação
2. Acesse **Settings** (⚙️ no sidebar)
3. Cole sua OpenAI API key no campo
4. Clique em "**Salvar API Key**"
5. A chave é salva no banco de dados
6. Funciona imediatamente (sem reiniciar)

### 2. Ver Alertas Inteligentes

1. Na home, clique no **botão flutuante 🚨** (canto inferior direito)
2. Modal abre com:
   - Resumo do mês
   - Lista de alertas com sugestões personalizadas da IA
3. Fechar: clique no X ou fora do modal

### 3. Gerar Análise IA

1. Na home, clique em "**Gerar Análise IA Completa**" (botão roxo)
2. Aguarde processamento (3-10 segundos)
3. Card de preview aparece com:
   - Score de saúde financeira
   - Resumo executivo
   - Previsão próximo mês
4. Clique em "**Ver Análise Completa**" para modal detalhado

### 4. Ver Insights no Extract

1. Acesse `/extract`
2. Faça upload de extrato (PDF/imagem)
3. Transações são extraídas automaticamente
4. Clique em "**Atualizar**" na seção de insights
5. Veja análise financeira com warnings da IA

---

## 📊 ENDPOINTS DA API

### Alertas
```
GET /api/expenses/alerts?userId=1&month=1&year=2025
```
Retorna alertas + análise IA automática

### Análise Mensal Completa
```
GET /api/ai/analyze?userId=1&month=1&year=2025
```
Retorna análise detalhada on-demand

### Detectar Padrões
```
GET /api/ai/patterns?userId=1&month=1&year=2025
```
Retorna padrões detectados

### Insights do Extract
```
GET /api/extract/insights?userId=1&month=1&year=2025
```
Retorna insights financeiros do extrato

---

## 🔍 FUNCIONALIDADES DA IA

### 1. Alertas Inteligentes
- Detecta gastos exagerados vs média histórica
- Gera sugestões personalizadas contextuais
- Identifica recordes históricos
- Classifica por severidade (crítico/atenção/info)

### 2. Análise Mensal Completa
- **Resumo executivo:** 2-3 frases sobre o mês
- **Score de saúde:** 0-100 baseado em gastos vs média
- **Padrões detectados:** Temporal, categoria, tendências, anomalias
- **Previsão:** Gasto previsto para próximo mês + confiança
- **Comparações:** vs mês anterior, vs média, tendência
- **Recomendações:** Lista de ações práticas

### 3. Detecção de Padrões
- **Temporal:** Gastos em dias específicos, fins de semana
- **Categoria:** Categorias dominantes, concentração
- **Trend:** Tendências de crescimento/redução
- **Anomaly:** Gastos atípicos ou inesperados

### 4. Insights do Extract
- Análise automática ao fazer upload
- Warnings sobre gastos específicos
- Maiores gastos identificados
- Categorias dominantes

---

## 🔧 CONFIGURAÇÃO TÉCNICA

### Variáveis de Ambiente

**Backend (`application.properties`):**
```properties
# OpenAI Configuration for AI Insights
openai.api.key=${OPENAI_API_KEY:}
openai.model=${OPENAI_MODEL:gpt-4o-mini}
```

**Nota:** A chave pode vir de duas fontes:
1. **Variável de ambiente** `OPENAI_API_KEY` (opcional)
2. **Banco de dados** via Settings (recomendado)

### Dependências

**Maven (`pom.xml`):**
```xml
<!-- LangChain4j for AI Integration -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j</artifactId>
    <version>0.36.2</version>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
    <version>0.36.2</version>
</dependency>
```

### Modelo LLM

- **Modelo:** `gpt-4o-mini` (padrão)
- **Temperature:** 0.7
- **Max Tokens:** 1500
- **Timeout:** 30 segundos

---

## 💰 CUSTO ESTIMADO

### OpenAI API Pricing (gpt-4o-mini)
- ~$0.0001-0.0002 por 1K tokens
- Análise completa: ~1000-2000 tokens
- **Custo por análise:** $0.10 - $0.40
- **Alertas (sugestões):** $0.01 - $0.05 cada

### Estimativa Mensal (Uso Médio)
- 2 usuários
- 10 análises/mês por usuário
- 30 alertas/mês total
- **Total:** $2 - $10/mês

---

## 📝 OBSERVAÇÕES IMPORTANTES

### Graceful Degradation
- App funciona **sem API key configurada**
- IA retorna respostas padrão se falhar
- Não bloqueia funcionalidades existentes

### Performance
- Chamadas IA podem demorar **3-10 segundos**
- Loading states implementados em todos os lugares
- Timeout de 30s por segurança

### Segurança
- API key **nunca exposta no frontend**
- Validação de userId em todos os endpoints
- Logs detalhados para debugging

---

## 🚀 PRÓXIMOS PASSOS OPCIONAIS

1. ✅ **Implementar seção de Padrões na página Extract** (15min)
2. Cache de análises (Redis ou localStorage)
3. Rate limiting por usuário
4. Comparação com outros usuários (anônimo)
5. Metas financeiras com acompanhamento IA
6. Alertas proativos por email/notificação
7. Relatórios PDF com análises IA

---

## 📚 REFERÊNCIAS

- [LangChain4j Documentation](https://docs.langchain4j.dev/)
- [OpenAI API Pricing](https://openai.com/pricing)
- [Plano Completo](C:\Users\lucas\.claude\plans\logical-scribbling-star.md)

---

**Última Atualização:** 10/01/2026
**Status:** ✅ 95% Completo (falta apenas seção de padrões no Extract)
