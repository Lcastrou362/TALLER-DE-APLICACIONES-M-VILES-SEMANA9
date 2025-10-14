# Despacho OSM — Semana 9

App Android que calcula el **costo de despacho** según la **distancia tienda–cliente** y el **monto de compra**, mostrando la ubicación del usuario en un **mapa (osmdroid)**.

**Reglas de negocio**
- **≥ $50.000 y ≤ 20 km → $0**
- **$25.000–$49.999 → $150 × km**
- **< $25.000 → $300 × km**

> Proyecto basado en el prototipo de la **Semana 6**; en esta entrega se agregan **mapa**, **ubicación** y **cálculo de despacho**. Incluye login y cierre de sesión (Firebase).

---

## Tecnologías
- Android Studio (API 34), Java + AndroidX  
- osmdroid 6.1.18  
- Google Play Services Location 21.3.0  
- Firebase Authentication (email/clave)

---

## Instalación
1. Clonar el repo y abrir en Android Studio.  
2. Sincronizar Gradle.  
3. (Opcional) Si usas Firebase, colocar `app/google-services.json`.  
4. Ejecutar en emulador o dispositivo.  
5. Para pruebas de distancia, en el emulador: **Extended Controls → Location**.

**Coordenada fija de tienda:** `lat -33.6846, lon -71.2153`.

---

## Funcionalidades
- Login / **Cerrar sesión** (Firebase).
- `MapView` (osmdroid) con zoom/drag multitáctil.
- Ubicación con **Fused Location**: último fix inmediato + lectura **HIGH_ACCURACY**.
- Distancia con **Haversine** y cálculo de **costo** según reglas.
- Manejo de **permisos de ubicación** en tiempo de ejecución.
- Validación de monto y mensajes de error claros (toasts).

---

## Historias de Usuario

**HU1 – Iniciar sesión**  
*Como* usuario, *quiero* ingresar con mi correo y contraseña *para* acceder a la app.  
**Criterios:** credenciales válidas → va al mapa; inválidas → toast de error; sesión persiste; hay botón **Cerrar sesión**.

**HU2 – Ver mi ubicación en el mapa**  
*Como* usuario, *quiero* ver mi ubicación actual *para* calcular la distancia de despacho.  
**Criterios:** al otorgar permisos aparece “Mi ubicación” y el mapa centra/ajusta zoom; si deniego, se informa y no calcula.

**HU3 – Calcular costo de despacho**  
*Como* usuario, *quiero* ingresar el monto y presionar **Calcular** *para* conocer el costo.  
**Criterios:** monto vacío/no numérico → toast; con datos válidos muestra Lat/Lon, Distancia (km) y Costo (CLP); aplica reglas.

**HU4 – Manejar permisos**  
*Como* usuario, *quiero* decidir si autorizo mi ubicación *para* controlar mis datos.  
**Criterios:** otorgo → calcula; deniego → informa y bloquea cálculo hasta aceptar.

**HU5 – Cerrar sesión**  
*Como* usuario autenticado, *quiero* cerrar sesión *para* finalizar el uso.  
**Criterios:** `signOut()` y regreso a Login; sin auto-login tras cerrar.

---

## Casos de Prueba (resumen)

| Caso | Monto (CLP) | Distancia | Costo esperado |
|---|---:|---:|---:|
| C1 | 55.000 | 10 km | **$0** |
| C2 | 55.000 | 21 km | **$6.300** |
| C3 | 30.000 | 10 km | **$1.500** |
| C4 | 30.000 | 57 km | **$8.550** |
| C5 | 15.000 | 10 km | **$3.000** |
| C6 | 15.000 | 57 km | **$17.100** |

---

## Estructura

