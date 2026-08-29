# Estágio de Build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# 1. Copia o pom.xml e baixa as dependências
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B

# 2. Copia todo o código-fonte do projeto
COPY . .

# 3. Garante a estrutura correta de diretórios do tema 'tms'
RUN mkdir -p /app/frontend/themes/tms /app/src/main/frontend/themes/tms && \
    if [ -d "src/main/frontend/themes/tms" ]; then \
        cp -r src/main/frontend/themes/tms/* /app/frontend/themes/tms/ 2>/dev/null || true; \
        cp -r src/main/frontend/themes/tms/* /app/src/main/frontend/themes/tms/ 2>/dev/null || true; \
    elif [ -d "frontend/themes/tms" ]; then \
        cp -r frontend/themes/tms/* /app/frontend/themes/tms/ 2>/dev/null || true; \
        cp -r frontend/themes/tms/* /app/src/main/frontend/themes/tms/ 2>/dev/null || true; \
    fi

# 4. Executa a preparação do frontend separadamente para resolver o Lumo e o Node
RUN --mount=type=cache,target=/root/.m2 mvn vaadin:prepare-frontend -Pproduction

# 5. Executa o empacotamento completo de produção final
RUN --mount=type=cache,target=/root/.m2 mvn package -Pproduction -DskipTests

# Estágio de Execução
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 10000
ENTRYPOINT ["java", "-Xms128m", "-Xmx384m", "-Dvaadin.productionMode=true", "-Dserver.port=10000", "-jar", "app.jar"]