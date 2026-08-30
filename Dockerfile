# Estágio de Build
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# 1. Copia o pom.xml e baixa as dependências
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B

# 2. Copia todo o código-fonte
COPY . .

# 3. Garante que a estrutura de diretórios do frontend e do tema 'tms' esteja perfeita para o Vite
RUN mkdir -p frontend/themes/tms && \
    if [ -d "src/main/frontend/themes/tms" ]; then \
        cp -r src/main/frontend/themes/tms/* frontend/themes/tms/ 2>/dev/null || true; \
    fi

# 4. Prepara os recursos e temas do Vaadin
RUN --mount=type=cache,target=/root/.m2 mvn vaadin:prepare-frontend -Pproduction

# 5. Executa o empacotamento de produção do Maven
RUN --mount=type=cache,target=/root/.m2 mvn package -Pproduction -DskipTests

# Estágio de Execução
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 10000
ENTRYPOINT ["java", "-Xms128m", "-Xmx384m", "-Dvaadin.productionMode=true", "-Dserver.port=10000", "-jar", "app.jar"]