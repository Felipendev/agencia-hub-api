# Recuperação da prévia F03

**Atualização em 20/10/2026:** este runbook é histórico. API #62 já integrada; deploy de produção `70ad7fb6-a722-46e5-99c8-50c226379a7b` SUCCESS, com logs confirmando V57 → V58 em public e inicialização concluída. Não há ajuste manual pendente dessa prévia para concluir a publicação do F03. Não aplicar estas variáveis à produção. Estado vigente: `migrations-reservadas-2026-10-15.md`.

## Diagnóstico confirmado

Produção permanece no deploy saudável F02, API `41a50d9ac3ca236c76e10163fed9ddb2bfec6ed3`, migrations até V57. Site e OpenAPI retornaram 200 durante a investigação. A falha examinada pertence à prévia efêmera `agencia-hub-api-pr-62`, deploy `49170051-8039-4514-9c89-ec236e082857`.

O histórico experimental antigo da prévia conflitou com a ordem V56/V57 de contratos. A tentativa de isolamento adicionou `SPRING_DATASOURCE_HIKARI_CONNECTION_INIT_SQL`, mas o binding falhou com `The configuration of the pool is sealed once started`. Essa orientação de configuração foi um erro da integração Codex; as falhas posteriores de inicialização não demonstram defeito no login ou perda dos dados de produção.

## Ajuste exato no painel

Abra o projeto `amusing-courtesy`, selecione o ambiente **agencia-hub-api-pr-62**, abra o serviço **agencia-hub-api** e a aba **Variables**. Confira o ambiente antes de editar. Não selecionar production ou o serviço Postgres.

1. Excluir apenas `SPRING_DATASOURCE_HIKARI_CONNECTION_INIT_SQL`.
2. Adicionar `SPRING_DATASOURCE_URL` com o valor literal abaixo, sem aspas externas:

```text
jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}?currentSchema=f03_v58_review,public
```

Os placeholders `${DB_HOST}`, `${DB_PORT}` e `${DB_NAME}` são resolvidos pela aplicação Spring a partir das variáveis já existentes. Não inserir senhas na URL. Não alterar DB_USER/DB_PASSWORD.

3. Conservar estas quatro variáveis já aplicadas:

```text
SPRING_FLYWAY_DEFAULT_SCHEMA=f03_v58_review
SPRING_FLYWAY_SCHEMAS=f03_v58_review
SPRING_FLYWAY_CREATE_SCHEMAS=true
SPRING_JPA_PROPERTIES_HIBERNATE_DEFAULT_SCHEMA=f03_v58_review
```

4. Salvar as duas alterações e clicar em **Deploy Changes** nessa prévia. Se o painel aplicar automaticamente, acompanhar o novo deploy em **Deployments**; não criar outro em produção.

Não apagar banco, esquema public, volume ou histórico de migrations. Não habilitar outOfOrder/ignore nem editar SQL V56/V57/V58. Não aplicar as variáveis da prévia à produção.

## Validação e limite

Teste `IsolatedSchemaStartupIntegrationTest` inicia a aplicação Spring real com PostgreSQL efêmero, Flyway/Hibernate no esquema `f03_v58_review` e URL JDBC com currentSchema. Confere V58 aplicada, schema corrente correto e um registro preexistente de public preservado. Também confirma que o histórico Flyway novo não foi criado em public.

Esse teste local não significa que a prévia já esteja recuperada. Após o ajuste, Codex deve conferir o novo deploy SUCCESS, logs de inicialização e Flyway, antes de merge/publicação API e UI. A API de produção ainda não recebeu F03.

Validação concluída: `mvn test -Dtest=IsolatedSchemaStartupIntegrationTest`, BUILD SUCCESS, 1 teste sem falhas/erros. Executado também com a URL literal e placeholders DB_HOST/DB_PORT/DB_NAME indicados acima, verificando a resolução do Spring. Os 161 testes anteriores da feature permanecem como evidência da revisão; este teste adicional verifica especificamente a configuração de inicialização.

O conector Railway disponível não oferece exclusão individual de variável. As tentativas anteriores de aplicação também foram recusadas pela ferramenta mesmo após autorização explícita. Por isso o ajuste de remoção foi descrito para execução no painel pelo usuário; não há nova aprovação de negócio pendente.
