# Contributing to AgenciaHub API

Obrigado por considerar contribuir com o backend do AgenciaHub! 🎉

## 📋 Código de Conduta

- Seja respeitoso e profissional
- Aceite feedback construtivo
- Foque no que é melhor para a comunidade
- Mostre empatia com outros membros

## 🚀 Como Contribuir

### Reportar Bugs

1. Verifique se o bug já foi reportado nas [Issues](../../issues)
2. Se não, crie uma nova issue com:
   - Título claro e descritivo
   - Passos para reproduzir
   - Comportamento esperado vs atual
   - Logs relevantes
   - Ambiente (Java version, Spring Boot version, OS)

### Sugerir Features

1. Verifique se a feature já foi sugerida
2. Crie uma issue com:
   - Descrição clara da feature
   - Motivação e casos de uso
   - Exemplos de API (request/response)

### Pull Requests

1. **Fork** o repositório
2. **Clone** seu fork localmente
3. **Crie uma branch** para sua feature/fix:
   ```bash
   git checkout -b feature/minha-feature
   # ou
   git checkout -b fix/meu-bug
   ```
4. **Faça suas mudanças** seguindo os padrões do projeto
5. **Adicione testes** para suas mudanças
6. **Commit** com mensagens claras:
   ```bash
   git commit -m "feat: adiciona endpoint de busca global"
   git commit -m "fix: corrige validação de email em Customer"
   ```
7. **Push** para seu fork:
   ```bash
   git push origin feature/minha-feature
   ```
8. **Abra um Pull Request** no repositório original

## 📝 Padrões de Código

### Estrutura de Pacotes

```
com.agenciahub.api
├── config          # Configurações (CORS, OpenAPI, etc)
├── controller      # REST Controllers
├── domain          # Enums e value objects
├── dto             # Request/Response DTOs
├── entity          # JPA Entities
├── exception       # Custom exceptions
├── repository      # Spring Data Repositories
└── service         # Business logic
```

### Convenções

- **Classes**: PascalCase
- **Métodos/Variáveis**: camelCase
- **Constantes**: UPPER_SNAKE_CASE
- **Pacotes**: lowercase
- Use **Lombok** para reduzir boilerplate
- Use **records** para DTOs (Java 17+)
- Documente endpoints com **@Operation** (OpenAPI)

### Exemplo de Controller

```java
@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
@Tag(name = "Customers")
public class CustomerController {
    
    private final CustomerService customerService;
    
    @GetMapping
    @Operation(summary = "List customers with optional filters")
    public List<CustomerResponse> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) CustomerStatus status) {
        return customerService.search(name, status);
    }
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create customer")
    public CustomerResponse create(@Valid @RequestBody CreateCustomerRequest request) {
        return customerService.create(request);
    }
}
```

### Exemplo de Service

```java
@Service
@RequiredArgsConstructor
public class CustomerService {
    
    private final CustomerRepository customerRepository;
    
    public List<CustomerResponse> search(String name, CustomerStatus status) {
        // Lógica de busca
        return customers.stream()
                .map(this::toResponse)
                .toList();
    }
    
    @Transactional
    public CustomerResponse create(CreateCustomerRequest request) {
        Customer customer = Customer.builder()
                .name(request.name())
                .email(request.email())
                .build();
        
        customer = customerRepository.save(customer);
        return toResponse(customer);
    }
    
    private CustomerResponse toResponse(Customer customer) {
        // Mapeamento
    }
}
```

### Exemplo de Entity

```java
@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, length = 255)
    private String name;
    
    @Column(nullable = false, length = 320)
    private String email;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CustomerStatus status;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
```

## 🗄️ Database Migrations

Use **Flyway** para migrations:

1. Crie arquivo em `src/main/resources/db/migration/`
2. Nomeie como `V{version}__{description}.sql`
   - Exemplo: `V4__add_notifications_table.sql`
3. Nunca modifique migrations já aplicadas
4. Use SQL idiomático do PostgreSQL

**Exemplo**:
```sql
-- V4__add_notifications_table.sql
CREATE TABLE notifications (
    id UUID NOT NULL PRIMARY KEY,
    user_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_notifications_user_id ON notifications (user_id);
CREATE INDEX idx_notifications_is_read ON notifications (is_read);
```

## 🧪 Testes

### Estrutura de Testes

```
src/test/java/com/agenciahub/api
├── controller      # Testes de controller (MockMvc)
├── service         # Testes de service (unitários)
└── repository      # Testes de repository (DataJpaTest)
```

### Exemplo de Teste de Service

```java
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
    
    @Mock
    private CustomerRepository customerRepository;
    
    @InjectMocks
    private CustomerService customerService;
    
    @Test
    void create_shouldSaveCustomer() {
        // Given
        CreateCustomerRequest request = new CreateCustomerRequest(
            "João Silva",
            "joao@example.com",
            "+55 11 98765-4321",
            "Europa",
            CustomerStatus.PROSPECT,
            ""
        );
        
        Customer savedCustomer = Customer.builder()
            .id(UUID.randomUUID())
            .name(request.name())
            .email(request.email())
            .build();
        
        when(customerRepository.save(any(Customer.class)))
            .thenReturn(savedCustomer);
        
        // When
        CustomerResponse response = customerService.create(request);
        
        // Then
        assertNotNull(response.id());
        assertEquals(request.name(), response.name());
        verify(customerRepository).save(any(Customer.class));
    }
}
```

### Rodar Testes

```bash
# Todos os testes
mvn test

# Testes de integração
mvn verify

# Com cobertura
mvn test jacoco:report
# Relatório em: target/site/jacoco/index.html

# Teste específico
mvn test -Dtest=CustomerServiceTest
```

## 📦 Commits

Usamos [Conventional Commits](https://www.conventionalcommits.org/):

- `feat:` Nova feature
- `fix:` Correção de bug
- `docs:` Mudanças na documentação
- `style:` Formatação
- `refactor:` Refatoração de código
- `test:` Adicionar/modificar testes
- `chore:` Tarefas de build, configs, etc

**Exemplos**:
```bash
feat: adiciona endpoint de notificações
fix: corrige validação de email em Customer
docs: atualiza README com instruções Docker
refactor: extrai lógica de mapeamento para classe separada
test: adiciona testes para QuotationService
chore: atualiza Spring Boot para 3.4.2
```

## 🔄 Processo de Review

1. Pelo menos 1 aprovação necessária
2. Testes devem passar
3. Cobertura de código mantida/melhorada
4. Código deve seguir os padrões
5. Migrations devem ser revisadas cuidadosamente
6. OpenAPI/Swagger deve estar atualizado

## 📚 Documentação

Ao adicionar features, atualize:

- `README.md` - Se muda setup ou uso básico
- OpenAPI annotations - Para novos endpoints
- Javadoc - Para lógica complexa
- `API_CONTRACT.md` (no frontend) - Para novos endpoints

## 🎯 Checklist para PRs

- [ ] Código segue os padrões do projeto
- [ ] Testes adicionados/atualizados
- [ ] Migrations criadas (se necessário)
- [ ] OpenAPI annotations adicionadas
- [ ] README atualizado (se necessário)
- [ ] Sem warnings do compilador
- [ ] Sem código comentado
- [ ] Logs apropriados adicionados

## 🐛 Debug Local

### Rodar com PostgreSQL

```bash
# 1. Subir PostgreSQL
docker compose up -d

# 2. Verificar se está rodando
docker compose ps

# 3. Rodar aplicação
export SPRING_PROFILES_ACTIVE=docker
mvn spring-boot:run

# 4. Acessar Swagger UI
open http://localhost:8080/api/v1/swagger-ui/index.html
```

### Logs

```bash
# Ver logs do PostgreSQL
docker compose logs -f postgres

# Ver logs da aplicação
# (stdout quando rodando com mvn spring-boot:run)
```

### Conectar ao PostgreSQL

```bash
docker compose exec postgres psql -U agenciahub -d agenciahub

# Comandos úteis:
\dt              # Listar tabelas
\d customers     # Descrever tabela
SELECT * FROM flyway_schema_history;  # Ver migrations aplicadas
```

## ❓ Dúvidas

- Abra uma [Discussion](../../discussions)
- Ou crie uma issue com label `question`

## 🙏 Agradecimentos

Toda contribuição é valiosa, seja código, documentação, testes ou feedback!

---

**Happy Coding!** 🚀
