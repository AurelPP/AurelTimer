@echo off
echo 🧪 === TEST R2CLIENT PUT ===
echo.

echo 📦 Compilation du projet...
call gradlew compileJava
if %errorlevel% neq 0 (
    echo ❌ Compilation échouée
    exit /b 1
)

echo.
echo 🚀 Lancement du test R2Put...
echo.

rem Lancer le test avec le classpath complet
call gradlew -q runClient --main-class=com.aureltimer.test.R2PutTest --args="--test-mode"

echo.
echo ✅ Test terminé !

