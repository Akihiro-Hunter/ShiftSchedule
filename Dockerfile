# ---- ビルド ----
# CSS/JS(src/main/resources/static)はGit管理済みなので、Node.jsは不要
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src src
# テストはDB接続が必要なためスキップ
RUN ./mvnw -B -q package -DskipTests

# ---- 実行 ----
FROM eclipse-temurin:25-jre
# 朝アシシフトPDFの日本語フォント(IPAゴシック)。ShiftPdfWriter が /usr/share/fonts/opentype/ipafont-gothic/ipag.ttf を探す
RUN apt-get update \
    && apt-get install -y --no-install-recommends fonts-ipafont-gothic \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --no-create-home app
WORKDIR /app
COPY --from=build /app/target/shift-schedule-*.jar app.jar
USER app
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -Duser.timezone=Asia/Tokyo"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
