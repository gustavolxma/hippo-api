# Etapa 1: compila a API e gera o .jar
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Baixa o Gradle e as dependências antes de copiar o código,
# para o Docker reaproveitar esse passo quando só o código mudar
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon > /dev/null

COPY src ./src
# Os testes rodam no GitHub Actions e no computador; aqui só gera o .jar
RUN ./gradlew bootJar --no-daemon -x test

# Etapa 2: imagem final, só com o Java e o .jar
FROM eclipse-temurin:17-jre
WORKDIR /app

# Horário de Brasília para datas e horas geradas no servidor
ENV TZ=America/Sao_Paulo
# Limita a memória do Java para caber na máquina gratuita da Oracle
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70"

RUN useradd --system --no-create-home hippo
COPY --from=build /app/build/libs/*.jar app.jar
USER hippo

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
