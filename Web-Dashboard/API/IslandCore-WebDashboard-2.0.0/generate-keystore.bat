@echo off
set /p PASS=Enter a long password for the TLS keystore: 
if not exist plugins\IslandCoreWebDashboard mkdir plugins\IslandCoreWebDashboard
keytool -genkeypair -alias islandcore-dashboard -keyalg RSA -keysize 3072 -validity 825 -storetype PKCS12 -keystore plugins\IslandCoreWebDashboard\dashboard.p12 -storepass %PASS% -keypass %PASS% -dname "CN=127.0.0.1" -ext "SAN=IP:127.0.0.1,DNS:localhost"
echo.
echo Keystore created. Put the same password in config.yml under tls.keystore-password.
pause
