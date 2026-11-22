# DuoKeyboard ⌨️✌️

Un teclado para Android que puedes **partir en dos** para escribir con los pulgares sin estirar el dedo.
Todo se configura a tu gusto, con un estilo inspirado en iOS.

## Cómo abrirlo y probarlo

1. Descomprime el proyecto y ábrelo en **Android Studio** (Ladybug o más nuevo).
2. Espera a que termine el *Gradle Sync* (descarga todo solo).
3. Conecta tu teléfono (con depuración USB) y dale **Run ▶**.
4. En la app: **Activar → Habilitar DuoKeyboard → Elegir teclado**.
5. Escribe en el campo de prueba.

> Requisitos: Android 8.0+ (minSdk 26). Se compila con SDK 35 y Java 17.

## Funciones

**Modos (cambia rápido desde la barra del teclado)**
- **Duo**: el teclado se parte en dos mitades. Puedes ajustar la separación, la **inclinación** (las mitades giran hacia tus pulgares) y la **curvatura** (las filas siguen el arco del pulgar).
- **Completo**: el clásico, con un arco opcional.
- **Una mano** (izquierda o derecha): con botones para cambiar de lado.
- **Duo automático en horizontal**.

**Estilo**
- 8 temas: iOS Oscuro, iOS Claro, Cristal, Neón, AMOLED, Atardecer, Menta y **Personalizado** (tú eliges fondo, teclas, texto y acento).
- 3 estilos de tecla: Sólida, Contorno y Minimal.
- Redondeo, espacio entre teclas, opacidad, tamaño de letra, sombra y bordes.

**Fondo**
- Una **foto de tu galería** (con el selector de fotos de Android, sin pedir permisos).
- **Imágenes satelitales**: 10 lugares listos (CDMX, Chichén Itzá, Everest, Sahara…) o tus propias coordenadas y zoom.
- **Esconder fondo** con un toque, además de oscurecer y desenfocar (desenfoque en Android 12+).

**Escritura**
- Burbuja al presionar (como en iOS) y vibración con fuerza ajustable.
- Sonido opcional.
- Mantén una tecla para ver los acentos (á, é, ñ, ¿, ¡, €…).
- Desliza sobre el espacio para mover el cursor.
- Doble toque en shift para bloquear mayúsculas.
- Mayúscula automática y punto con doble espacio.
- La tecla Enter cambia según la app (buscar, enviar, ir, listo…).

## Estructura

```
app/src/main/java/com/frychanel/duokeyboard/
├── data/
│   ├── KeyboardConfig.kt     ← todas las opciones + guardado (JSON)
│   └── BackgroundStore.kt    ← foto de galería e imágenes satelitales
├── keyboard/
│   ├── Layouts.kt            ← distribución QWERTY español, símbolos y acentos
│   ├── Themes.kt             ← temas de color
│   ├── KeyboardController.kt ← lógica: escribir, borrar, shift, enter…
│   ├── KeyboardUi.kt         ← el teclado dibujado con Jetpack Compose
│   └── KeyIcons.kt           ← íconos estilo iOS
├── ime/DuoKeyboardService.kt ← el servicio de teclado de Android
└── settings/                 ← app de ajustes con vista previa en vivo
```

## Notas

- **Privacidad:** no se guarda ni se envía nada de lo que escribes. Solo se usa internet para descargar las imágenes satelitales.
- **Imágenes satelitales:** vienen de Esri World Imagery. Antes de publicar en Play Store, revisa sus términos de uso (o cámbialo por Mapbox o MapTiler con tu propia API key) en `BackgroundStore.descargarSatelite`.

## Próximos pasos

- Sugerencias y autocorrección.
- Emojis y GIFs.
- Escritura deslizando.
- Modo flotante.
- Más idiomas.
- **Versión para iPhone** (Swift + Keyboard Extension).
