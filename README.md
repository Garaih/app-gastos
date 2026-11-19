# app-gastos


Documentación y decisiones: repositorio `Garaih/gestor-central` (`apps/app-gastos.md`).

## Stack
Kotlin (integrado en AGP 9), Jetpack Compose, Room (a partir del paso 4). minSdk 26, targetSdk 36.

## Abrir en Android Studio
1. `git clone https://github.com/Garaih/app-gastos.git`
2. Android Studio → File → Open → carpeta `app-gastos` (la raíz, no `app/`).
3. Deja que haga Gradle Sync (descarga Gradle 9.4.1 y dependencias la primera vez).
4. Si pide instalar SDK Platform 36 o Build Tools, acepta.
5. Conecta el móvil con depuración USB y pulsa Run ▶.

JDK: usa el que trae Android Studio (17 o superior).

## Estructura
```
app/src/main/java/com/garaih/appgastos/
  MainActivity.kt   punto de entrada (Compose)
  captura/          servicio de notificaciones (paso 3)
  datos/            modelo Gasto, parser, Room (paso 4)
  ui/               pantallas y tema
```

## Privacidad
- Sin permiso de INTERNET y con `allowBackup=false`: los datos no salen del móvil.
- Nunca subas al repositorio importes, comercios ni texto de notificaciones reales.
