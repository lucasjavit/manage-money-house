# Configuração do IDE

## Problema: "non-project file" no Java

Se você está vendo o erro "ManageHouseMoneyApplication.java is a non-project file", siga estes passos:

## Solução para VS Code / Cursor

### 1. Instalar Extensões Java

Certifique-se de ter instalado:
- **Extension Pack for Java** (vscjava.vscode-java-pack)
- **Spring Boot Extension Pack** (opcional, mas recomendado)

### 2. Recarregar o Projeto

1. Abra a paleta de comandos (`Ctrl+Shift+P` ou `Cmd+Shift+P`)
2. Digite: `Java: Clean Java Language Server Workspace`
3. Selecione e confirme
4. Recarregue a janela (`Ctrl+R` ou `Cmd+R`)

### 3. Configurar o Workspace

O arquivo `.vscode/settings.json` já está configurado. Se ainda não funcionar:

1. Abra a paleta de comandos
2. Digite: `Java: Configure Java Runtime`
3. Verifique se o Java 21 está configurado

### 4. Forçar Reimportação do Maven

1. Abra a paleta de comandos
2. Digite: `Java: Rebuild Projects`
3. Aguarde a conclusão

### 5. Verificar a Estrutura do Projeto

O projeto deve ter esta estrutura:
```
ManageHouseMoney/
├── backend/
│   ├── pom.xml          ← Arquivo Maven principal
│   └── src/
│       └── main/
│           └── java/
│               └── com/managehouse/money/
└── .vscode/
    └── settings.json    ← Configurações do workspace
```

## Solução Alternativa: Usar Maven Diretamente

Se o IDE continuar com problemas, você pode executar o projeto diretamente via Maven:

```bash
cd ManageHouseMoney/backend
mvn spring-boot:run
```

## Verificar se está Funcionando

1. Abra qualquer arquivo `.java` no backend
2. Verifique se há erros de compilação (não deve haver)
3. Verifique se o autocomplete está funcionando
4. Tente executar a aplicação pelo IDE

## Troubleshooting

### Se ainda não funcionar:

1. **Feche e reabra o VS Code/Cursor**
2. **Delete a pasta `.vscode` e recrie** (ou use os arquivos já criados)
3. **Verifique se o Java 21 está instalado**:
   ```bash
   java -version
   ```
4. **Verifique se o Maven está instalado**:
   ```bash
   mvn -version
   ```

### Comandos Úteis

- `Java: Show Build Job Status` - Ver status do build
- `Java: Reload Projects` - Recarregar projetos
- `Java: Clean Java Language Server Workspace` - Limpar cache

