# Music School

Sistema desktop para gestão de alunos de escolas de música, desenvolvido em JavaFX com persistência local em SQLite.

## Funcionalidades

- **Cadastro completo de alunos** — dados pessoais, instrumento, nível, professor e foto
- **Controle de pagamentos** — histórico mensal com status Pago / Pendente / Atrasado e geração automática de meses
- **Registro de aulas** — controle de presença e frequência percentual
- **Filtros e busca** — por nome, instrumento, professor ou status
- **Gráficos** — alunos por instrumento e por status
- **Export `.xlsx`** — lista completa de alunos em planilha
- **Backup** — cópia do banco para OneDrive ou pasta manual

## Pré-requisitos

- Java 17+ (recomendado: [Azul ZuluFX 17](https://www.azul.com/downloads/) — já inclui JavaFX)
- Maven 3.9+

## Como rodar

```powershell
cd C:\Users\arthu\projects\music-school
mvn javafx:run
```

## Como gerar JAR executável

```powershell
mvn clean package
java -jar target\music-school-1.0.0.jar
```

## Dados locais

- **Banco de dados:** `C:\Users\{usuario}\music-school\school.db`
- **Fotos dos alunos:** caminhos referenciados no banco (arquivos permanecem em seus locais originais)

## Stack

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 17 | Linguagem principal |
| JavaFX | 21.0.2 | Interface gráfica |
| SQLite JDBC | 3.45.1 | Persistência local |
| Apache POI | 5.2.5 | Export `.xlsx` |
| JUnit 5 | 5.10.2 | Testes automatizados |
| Maven | 3.9.6 | Build e dependências |

## Estrutura do projeto

```
src/main/java/com/arthas/musicschool/
├── model/          Student, Payment, Lesson
├── repository/     DatabaseManager, StudentRepository, PaymentRepository, LessonRepository
├── service/        StudentService, PaymentService, LessonService, SpreadsheetService, BackupService
└── controller/     MainController, StudentDialog, PaymentController, LessonController, ChartController
```
