# Backlog Backend — Huella360 Plataforma de Interoperabilidad

> Documento maestro del backlog de transformación. Autocontenido.

---

## 1. Contexto y Objetivo

Huella360 actualmente es un **marketplace veterinario SaaS** funcional con 20+ módulos. El objetivo estratégico es **transformarlo en la plataforma de interoperabilidad que conecte todo el ecosistema de salud animal**, potenciada con IA y Big Data.

### De Marketplace a Plataforma

| Nivel Actual | Nivel Objetivo |
|---|---|
| App para dueños y veterinarias | Infraestructura que conecta todo el ecosistema |
| Historial clínico básico (1 vet ve sus mascotas) | Historial clínico **único, compartido y estandarizado** |
| Datos aislados por empresa | **Big Data** para predicción epidemiológica |
| IA con 1 endpoint básico | IA completa: chatbot, predicción, alertas, análisis de imágenes |
| Sin integración externa | **APIs de interoperabilidad** para laboratorios, municipalidades, farmacias, aseguradoras |

### Problema que Resuelve

- **60%+ de mascotas en Latinoamérica** no tienen historial clínico digital unificado
- Cada veterinaria guarda registros aislados → tratamientos repetidos, vacunas perdidas, diagnósticos tardíos
- No hay detección temprana de enfermedades ni alertas epidemiológicas
- Abandono animal por seguimiento deficiente
- Imposible predecir brotes por zona geográfica

---

## 2. Alcance de la Transformación

### IN (Entra en la transformación)

**Fase 1 — Historial Clínico Unificado**
- Modelo de datos estandarizado (hl7-fhir-inspired)
- Historial clínico compartido entre veterinarios (con consentimiento)
- Registro completo: vacunas, tratamientos, alergias, cirugías, análisis
- API de lectura/escritura del historial
- Consentimiento del dueño para compartir datos

**Fase 2 — Plataforma de Datos**
- Data Lake simplificado ( PostgreSQL + tablas de hechos)
- ETL básico para agregación de datos
- Dashboard epidemiológico (enfermedades por zona, tendencias)
- Reportes de salud pública animal

**Fase 3 — IA y Predicción**
- Chatbot veterinario con RAG (historial + base de conocimiento)
- Predicción de enfermedades por especie/raza/zona
- Alertas automáticas (vacunas vencidas, seguimientos pendientes)
- Detección de patrones anómalos (posibles brotes)
- Análisis de imágenes (lesiones, piel, etc.)

**Fase 4 — Ecosistema Conectado**
- API pública para laboratorios (envío/recección de análisis)
- Integración con municipalidades (registro de mascotas, denuncias)
- Conexión con refugios (seguimiento de adopciones)
- Integración con farmacias (dispensación de medicamentos)
- Conexión con aseguradoras (coberturas, reclamos)
- API de terceros documentada (OpenAPI)

### OUT (Post-transformación, no se construye ahora)

- App móvil nativa (solo backend)
- Telemedicina avanzada con video (ya hay base con Jitsi)
- Blockchain para trazabilidad de medicamentos
- IoT directo (collares GPS, sensores) — solo integración via API
- Marketplace de seguros (solo integración básica)
- Validación contra RENIEC (ya configurado, pendiente activar)

---

## 3. Casos de Uso Clave

### 3.1 Dr. Carlos — Veterinario General en Lima

**Dolor:** Atiende 20 mascotas/día. Cada una tiene historia en su cuaderno o en Excel. Cuando llega una mascota que atendió otro veterinario, no tiene acceso al historial. Repite exámenes, pregunta vacunas de memoria.

**Con Huella360:**
1. Escanea el QR de la mascota o busca por DNI del dueño
2. Ve el historial completo: vacunas, tratamientos, alergias, análisis previos
3. Registra la consulta actual → se guarda en el historial unificado
4. La IA le sugiere: "Esta mascota no ha recibido vacuna antirrábica en 14 meses, considerar refuerzo"
5. Programa seguimiento → el dueño recibe recordatorio automático

### 3.2 Municipalidad de San Isidro — Control Epidemiológico

**Dolor:** No tiene datos consolidados de salud animal en su jurisdicción. Cuando hay un brote de moquillo, se entera tarde. No puede identificar zonas de riesgo.

**Con Huella360:**
1. Accede al dashboard epidemiológico municipal
2. Ve mapa de calor: "12 casos de moquillo en el distrito último mes, concentrados en Surco"
3. Recibe alerta automática: "Incremento del 300% en consultas por diarrea viral en distritos del sur"
4. Puede emitir alertas a veterinarios de la zona
5. Genera reportes para la Dirección de Salud

### 3.3 Laboratorio VetLab — Análisis Clínicos

**Dolor:** El veterinario pide un análisis, imprime una orden en papel, el dueño la lleva. Los resultados llegan por WhatsApp o email. No quedan registrados en el historial.

**Con Huella360:**
1. Veterinario genera orden de análisis desde Huella360
2. VetLab recibe la orden via API (o ve un portal)
3. Realiza el análisis, sube resultados via API
4. Resultados se guardan automáticamente en el historial clínico de la mascota
5. Veterinario y dueño son notificados
6. La IA analiza tendencias: "Los niveles de creatinina de Max han subido 3 meses consecutivos"

### 3.4 Dueño — María con su perro Max

**Dolor:** Max tiene alergias, pero cuando lo lleva a un veterinario nuevo, tiene que recordar todo de memoria. Perdió el carnet de vacunas. No sabe si el tratamiento anterior funcionó.

**Con Huella360:**
1. Abre Huella360 → ve historial completo de Max
2. Comparte el historial con el nuevo veterinario (consentimiento temporal)
3. Recibe alertas: "Vacuna antirrábica vence en 15 días"
4. Puede ver análisis de laboratorio online
5. Si Max se pierde, su información está centralizada y accesible

### 3.5 Farmacia VetFarma — Dispensación

**Dolor:** El veterinario receta un medicamento, el dueño va a la farmacia y compra lo que puede. No hay seguimiento de dispensación.

**Con Huella360:**
1. Veterinario genera receta digital en Huella360
2. VetFarma recibe la receta via API
3. Dispensa el medicamento y registra en el sistema
4. Historial se actualiza: "Medicamento dispensado: Amoxicilina 500mg, 14 días"
5. La IA detecta: "El paciente ha sido recetado antibióticos 3 veces en 6 meses, considerar evaluación de resistencia"

### 3.6 Aseguradora PetSure — Coberturas

**Dolor:** No tiene datos de salud de las mascotas aseguradas. Evalúa reclamos manualmente. No puede ofrecer primas basadas en riesgo real.

**Con Huella360:**
1. PetSure accede al historial (con autorización del dueño) via API
2. Ve diagnósticos, tratamientos, vacunas al día
3. Puede calcular prima basada en raza, edad, historial
4. Al recibir un reclamo, verifica que el tratamiento esté cubierto y que la mascota tenga vacunas al día
5. Reduce fraudes y mejora la experiencia del cliente

---

## 4. Estado Actual del Backend

### Stack Actual
- Spring Boot 3.5.10, Java 17
- PostgreSQL 16 + Flyway (34 migraciones)
- Spring Security + JWT (jjwt 0.12.5) + Auth0
- Redis (caché + WebSocket relay)
- Resend (email), Cloudinary (imágenes), MercadoPago (pagos)
- OpenAI gpt-4o-mini (1 endpoint: health-alerts)
- JasperReports (PDF)
- WebSockets STOMP (GPS tracking)
- Bucket4j (rate limiting)
- Testcontainers (test setup)

### Módulos Completos (20+)
auth, user, client, company, veterinarian, pet, medical_record, catalog, sales, payment, appointment, delivery, adoption, notification, subscription, points, complaint, teleconsulta, leads, referral, reminder, dashboard, admin

### Lo que Falta para la Transformación

| Área | Estado Actual | Estado Necesario |
|------|--------------|------------------|
| Historial clínico | Básico: diagnóstico, tratamiento, notas, peso | Completo: vacunas, alergias, cirugías, análisis, tratamientos con fechas |
| IA | 1 endpoint (health-alerts con OpenAI) | Chatbot RAG, predicción, alertas, análisis de imágenes |
| Datos | Aislados por empresa | Agregados, analizables, con tendencias |
| Integración externa | Ninguna API pública | APIs para lab, muni, farmacias, aseguradoras |
| Consentimiento | No existe | Framework de consentimiento para compartir datos |
| Notificaciones avanzadas | Email básico | Alertas inteligentes, recordatorios predictivos |
| Reportes | Dashboard básico de empresa | Dashboard epidemiológico, reportes de salud pública |

---

## 5. Modelo de Dominio Objetivo

### 5.1 Diagrama de Relaciones (Expandido)

```
                              ┌─────────────────────────┐
                              │      Consentimiento     │
                              │   (NUEVO - Core)        │
                              └───────────┬─────────────┘
                                          │
┌──────────┐    1:1    ┌──────────┐  1:N  ┌──────────────────┐
│   User   │◄─────────►│ Mascota  │◄─────►│ HistoriaClinica  │
│(ya existe)│          │(ya existe)│       │  (EXPANDIR)      │
└────┬─────┘           └──────────┘       └────────┬─────────┘
     │                                              │
     │ 1:1        ┌─────────────────────┐           │ 1:N
     ├───────────►│ PerfilSalud         │           │
     │            │ (NUEVO)             │           ▼
     │            │ - pesoAtual         │   ┌──────────────────┐
     │            │ - especie           │   │ RegistroVacuna   │
     │            │ - raza              │   │ (NUEVO)          │
     │            │ - alergias[]        │   │ - vacuna         │
     │            │ - condiciones[]     │   │ - fecha          │
     │            │ - historialFamiliar │   │ - lote           │
     │            └─────────────────────┘   │ - veterinario    │
     │                                      └──────────────────┘
     │
     │ 1:N        ┌─────────────────────┐
     └───────────►│ AlertaIA            │
                  │ (NUEVO)             │
                  │ - tipo              │
                  │ - severidad         │
                  │ - mensaje           │
                  │ - leida             │
                  │ - accionRecomendada │
                  └─────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    CAPA DE INTEROPERABILIDAD                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ Laboratorio  │  │Municipalidad │  │  Farmacia    │     │
│  │   (NUEVO)    │  │   (NUEVO)    │  │   (NUEVO)    │     │
│  │ - nombre     │  │ - nombre     │  │ - nombre     │     │
│  │ - ruc        │  │ - codigo     │  │ - ruc        │     │
│  │ - api_key    │  │ - distrito   │  │ - api_key    │     │
│  │ - endpoint   │  │ - endpoint   │  │ - endpoint   │     │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘     │
│         │                 │                 │               │
│         ▼                 ▼                 ▼               │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              InteroperabilityGateway                 │   │
│  │  - exchangeHL7()                                    │   │
│  │  - validateFHIR()                                   │   │
│  │  - logIntercambio()                                 │   │
│  └─────────────────────────────────────────────────────┘   │
│                          │                                  │
│                          ▼                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                 DataLake / Analytics                 │   │
│  │  - HechosSaludAnimal (tabla de hechos)              │   │
│  │  - MetricasZona (agregaciones)                      │   │
│  │  - TendenciasEnfermedad (series temporales)         │   │
│  └─────────────────────────────────────────────────────┘   │
│                          │                                  │
│                          ▼                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                    IA / ML Engine                    │   │
│  │  - PredictiveService (enfermedades por zona/raza)   │   │
│  │  - EpidemiologicalAlertService (brotes)             │   │
│  │  - ChatbotRAGService (asistente con historial)      │   │
│  │  - ImageAnalysisService (lesiones, piel)            │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 5.2 Nuevas Entidades

#### PerfilSalud (NUEVO)
| Campo | Tipo | Notas |
|-------|------|-------|
| id | Long | PK |
| mascota_id | Long | FK → mascotas, UNIQUE |
| peso_actual_kg | BigDecimal | Último peso registrado |
| especie | String | Perro, gato, etc. |
| raza | String | |
| color | String | |
| fecha_nacimiento | Date | |
| esterilizado | boolean | |
| alergias | JSONB | Array de alergias conocidas |
| condiciones_cronicas | JSONB | Array de condiciones |
| historial_familiar | JSONB | Enfermedades familiares |
| seguro_id | String | Referencia a aseguradora (futuro) |
| created_at | Timestamp | |
| updated_at | Timestamp | |

#### RegistroVacuna (NUEVO)
| Campo | Tipo | Notas |
|-------|------|-------|
| id | Long | PK |
| mascota_id | Long | FK → mascotas |
| vacuna_id | Long | FK → CatalogoVacuna |
| fecha_aplicacion | Date | |
| fecha_proximo_refuerzo | Date | |
| lote | String | Lote de la vacuna |
| dosis | Integer | Número de dosis |
| veterinario_id | Long | FK → usuarios |
| empresa_id | Long | FK → empresas (quién aplicó) |
| notas | String | |
| created_at | Timestamp | |

#### CatalogoVacuna (NUEVO)
| Campo | Tipo | Notas |
|-------|------|-------|
| id | Long | PK |
| nombre | String | Unique |
| especie_objetivo | String | Perro, gato, ambos |
| frecuencia_refuerzo_meses | Integer | Default 12 |
| enfermedad_previene | String | |
| activo | boolean | |

#### RegistroAlergia (NUEVO)
| Campo | Tipo | Notas |
|-------|------|-------|
| id | Long | PK |
| mascota_id | Long | FK → mascotas |
| alergia | String | Tipo de alergia |
| severidad | Enum | LEVE, MODERADA, GRAVE |
| detectado_por | Long | FK → usuarios (veterinario) |
| fecha_deteccion | Date | |
| notas | String | |
| activo | boolean | |

#### RegistroCirugia (NUEVO)
| Campo | Tipo | Notas |
|-------|------|-------|
| id | Long | PK |
| mascota_id | Long | FK → mascotas |
| tipo_cirugia | String | |
| fecha | Date | |
| veterinario_id | Long | FK → usuarios |
| empresa_id | Long | FK → empresas |
| resultado | String | |
| complicaciones | String | nullable |
| notas | String | |

#### RegistroAnalisis (NUEVO)
| Campo | Tipo | Notas |
|-------|------|-------|
| id | Long | PK |
| mascota_id | Long | FK → mascotas |
| laboratorio_id | Long | FK → laboratorios |
| tipo_analisis | String | Hemograma, urianálisis, etc. |
| fecha_solicitud | Date | |
| fecha_resultado | Date | nullable |
| resultado_json | JSONB | Resultados estructurados |
| archivo_url | String | PDF del resultado |
| veterinario_solicitante_id | Long | FK → usuarios |
| estado | Enum | SOLICITADO, EN_PROCESO, COMPLETADO |
| created_at | Timestamp | |

#### Consentimiento (NUEVO)
| Campo | Tipo | Notas |
|-------|------|-------|
| id | Long | PK |
| mascota_id | Long | FK → mascotas |
| duenio_id | Long | FK → usuarios |
| destinatario_tipo | Enum | VETERINARIO, LABORATORIO, MUNICIPALIDAD, ASEGURADORA, FARMACIA |
| destinatario_id | Long | ID del destinatario |
| ambito | Enum | COMPLETO, ESPECIFICO, LIMITADO |
| campos_permitidos | JSONB | Qué datos puede ver |
| fecha_inicio | Timestamp | |
| fecha_fin | Timestamp | nullable (null = indefinido) |
| revocado | boolean | |
| created_at | Timestamp | |

#### AlertaIA (NUEVO)
| Campo | Tipo | Notas |
|-------|------|-------|
| id | Long | PK |
| mascota_id | Long | FK → mascotas |
| usuario_id | Long | FK → usuarios (dueño) |
| tipo | Enum | VACUNA, SEGUIMIENTO, PESO, ENFERMEDAD, BROTE, PREVENTIVA |
| severidad | Enum | INFO, WARNING, CRITICAL |
| titulo | String | |
| mensaje | String | |
| recomendacion | String | |
| accion_recomendada | String | |
| leida | boolean | Default false |
| procesada | boolean | Default false |
| fecha_expiracion | Timestamp | |
| created_at | Timestamp | |

#### Laboratorio (NUEVO)
| Campo | Tipo | Notas |
|-------|------|-------|
| id | Long | PK |
| nombre | String | |
| ruc | String | Unique |
| direccion | String | |
| telefono | String | |
| email | String | |
| api_key | String | Hash, para autenticación |
| endpoint_callback | String | URL donde envía resultados |
| activo | boolean | |
| verified | boolean | |
| created_at | Timestamp | |

#### Municipalidad (NUEVO)
| Campo | Tipo | Notas |
|-------|------|-------|
| id | Long | PK |
| nombre | String | Nombre del distrito/ciudad |
| codigo_municipalidad | String | Unique |
| region | String | |
| distrito | String | |
| contacto_email | String | |
| contacto_telefono | String | |
| endpoint_callback | String | URL para recibir alertas |
| activo | boolean | |
| created_at | Timestamp | |

#### Farmacia (NUEVO)
| Campo | Tipo | Notas |
|-------|------|-------|
| id | Long | PK |
| empresa_id | Long | FK → empresas (si es parte del ecosistema) |
| nombre | String | |
| ruc | String | Unique |
| direccion | String | |
| telefono | String | |
| api_key | String | Hash |
| activo | boolean | |
| created_at | Timestamp | |

#### Aseguradora (NUEVO)
| Campo | Tipo | Notas |
|-------|------|-------|
| id | Long | PK |
| nombre | String | |
| ruc | String | Unique |
| endpoint_callback | String | |
| api_key | String | Hash |
| activo | boolean | |
| created_at | Timestamp | |

#### IntercambioHL7 (NUEVO - Auditoría)
| Campo | Tipo | Notas |
|-------|------|-------|
| id | Long | PK |
| origen_tipo | String | VETERINARIO, LABORATORIO, etc. |
| origen_id | Long | |
| destino_tipo | String | |
| destino_id | Long | |
| tipo_dato | String | HISTORIAL, ANALISIS, VACUNA, etc. |
| formato | String | HL7_FHIR, JSON, XML |
| payload_hash | String | Hash del intercambio |
| exitoso | boolean | |
| error_mensaje | String | nullable |
| created_at | Timestamp | |

#### HechoSaludAnimal (NUEVO - Data Lake)
| Campo | Tipo | Notas |
|-------|------|-------|
| id | Long | PK |
| fecha | Date | |
| mascota_id | Long | |
| especie | String | |
| raza | String | |
| edad_anos | Integer | |
| sexo | String | |
| distrito | String | |
| region | String | |
| enfermedad_diagnosticada | String | |
| tipo_tratamiento | String | |
| costo_tratamiento | BigDecimal | |
| vacuna_aplicada | String | nullable |
| empresa_id | Long | |
| veterinario_id | Long | |

#### AlertaEpidemiologica (NUEVO)
| Campo | Tipo | Notas |
|-------|------|-------|
| id | Long | PK |
| enfermedad | String | |
| region | String | |
| distrito | String | |
| periodo_inicio | Date | |
| periodo_fin | Date | |
| casos_detectados | Integer | |
| umbral_superado | boolean | |
| nivel_riesgo | Enum | BAJO, MEDIO, ALTO, CRITICO |
| notificado_a | JSONB | Lista de entidades notificadas |
| resuelto | boolean | |
| created_at | Timestamp | |

### 5.3 Entidades Existentes a Modificar

**HistoriaClinica** — Expandir significativamente:
```java
// AGREGAR CAMPOS:
private LocalDate fechaProximoControl;
private BigDecimal temperatura;
private String frecuenciaCardiaca;
private String frecuenciaRespiratoria;
private String estadoHidratacion;
private BigDecimal; // index de condición corporal
private String[] vacunasPendientes;
private Boolean requiereSeguimiento;
private LocalDate fechaProximoSeguimiento;
```

**Mascota** — Agregar:
```java
private PerfilSalud perfilSalud; // 1:1
private List<RegistroVacuna> vacunas;
private List<RegistroAlergia> alergias;
private List<RegistroCirugia> cirugias;
private List<RegistroAnalisis> analisis;
```

---

## 6. Estructura de Paquetes Propuesta

```
com.vet_saas
├── VetSaasApplication.java
├── config/                          (existente, expandir)
│   ├── ... (10 existentes)
│   ├── DataLakeConfig.java          (NUEVO)
│   ├── InteroperabilityConfig.java  (NUEVO)
│   └── AiEngineConfig.java          (NUEVO)
├── security/                        (existente)
├── core/                            (existente, expandir)
│   ├── exceptions/
│   ├── response/
│   ├── service/
│   ├── utils/
│   └── interoperability/            (NUEVO)
│       ├── InteroperabilityGateway.java
│       ├── Hl7FhirMapper.java
│       └── ConsentValidator.java
├── modules/
│   ├── (20+ módulos existentes)
│   │
│   ├── clinical_history/            (NUEVO - reemplaza medical_record básico)
│   │   ├── model/
│   │   │   ├── PerfilSalud.java
│   │   │   ├── RegistroVacuna.java
│   │   │   ├── CatalogoVacuna.java
│   │   │   ├── RegistroAlergia.java
│   │   │   ├── RegistroCirugia.java
│   │   │   └── RegistroAnalisis.java
│   │   ├── repository/
│   │   ├── service/
│   │   │   ├── ClinicalHistoryService.java
│   │   │   ├── VaccinationService.java
│   │   │   └── AllergyService.java
│   │   ├── controller/
│   │   │   ├── ClinicalHistoryController.java
│   │   │   └── VaccinationController.java
│   │   └── dto/
│   │
│   ├── consent/                     (NUEVO)
│   │   ├── model/
│   │   │   ├── Consentimiento.java
│   │   │   └── AmbitoConsentimiento.java
│   │   ├── repository/
│   │   ├── service/
│   │   │   └── ConsentService.java
│   │   ├── controller/
│   │   │   └── ConsentController.java
│   │   └── dto/
│   │
│   ├── lab/                         (NUEVO)
│   │   ├── model/
│   │   │   ├── Laboratorio.java
│   │   │   └── OrdenAnalisis.java
│   │   ├── repository/
│   │   ├── service/
│   │   │   ├── LabService.java
│   │   │   └── LabIntegrationService.java
│   │   ├── controller/
│   │   │   ├── LabController.java
│   │   │   └── LabPublicController.java (webhook resultados)
│   │   └── dto/
│   │
│   ├── municipality/                (NUEVO)
│   │   ├── model/
│   │   │   └── Municipalidad.java
│   │   ├── repository/
│   │   ├── service/
│   │   │   └── MunicipalityService.java
│   │   ├── controller/
│   │   │   └── MunicipalityController.java
│   │   └── dto/
│   │
│   ├── pharmacy/                    (NUEVO)
│   │   ├── model/
│   │   │   └── Farmacia.java
│   │   ├── repository/
│   │   ├── service/
│   │   │   └── PharmacyService.java
│   │   ├── controller/
│   │   │   └── PharmacyController.java
│   │   └── dto/
│   │
│   ├── insurance/                   (NUEVO)
│   │   ├── model/
│   │   │   └── Aseguradora.java
│   │   ├── repository/
│   │   ├── service/
│   │   │   └── InsuranceService.java
│   │   ├── controller/
│   │   │   └── InsuranceController.java
│   │   └── dto/
│   │
│   ├── epidemiology/                (NUEVO)
│   │   ├── model/
│   │   │   ├── HechoSaludAnimal.java
│   │   │   ├── AlertaEpidemiologica.java
│   │   │   └── MetricaZona.java
│   │   ├── repository/
│   │   ├── service/
│   │   │   ├── EpidemiologicalService.java
│   │   │   ├── DataLakeService.java
│   │   │   └── AlertService.java
│   │   ├── controller/
│   │   │   └── EpidemiologicalController.java
│   │   ├── scheduler/
│   │   │   └── EpidemiologicalScheduler.java
│   │   └── dto/
│   │
│   ├── ia/                          (existente, EXPANDIR)
│   │   ├── model/
│   │   │   ├── IaUsage.java         (existente)
│   │   │   ├── PrediccionEnfermedad.java (NUEVO)
│   │   │   └── AnalisisImagen.java  (NUEVO)
│   │   ├── service/
│   │   │   ├── IaService.java       (existente, refactorizar)
│   │   │   ├── ChatbotRAGService.java (NUEVO)
│   │   │   ├── PredictiveService.java (NUEVO)
│   │   │   ├── ImageAnalysisService.java (NUEVO)
│   │   │   └── RecommendationService.java (NUEVO)
│   │   ├── controller/
│   │   │   ├── IaController.java    (existente, expandir)
│   │   │   └── ChatbotController.java (NUEVO)
│   │   └── dto/
│   │
│   └── interoperability/            (NUEVO - capa central)
│       ├── model/
│       │   ├── IntercambioHL7.java
│       │   └── ApiKey.java
│       ├── service/
│       │   ├── InteroperabilityGateway.java
│       │   ├── Hl7FhirMapper.java
│       │   └── AuditInteropService.java
│       ├── filter/
│       │   └── ApiKeyAuthFilter.java
│       └── dto/
│
└── shared/
    ├── audit/
    ├── error/
    ├── security/
    ├── ratelimit/
    ├── mail/
    ├── cache/
    └── datalake/                    (NUEVO)
        ├── EtlService.java
        └── FactTableUpdater.java
```

---

## 7. Backlog por Fases

### Convenciones

- **ID:** H360-NN correlativo
- **Dificultad:** S (≤ 1 día), M (2-3 días), L (4-5 días), XL (1-2 semanas)
- **Etiquetas:**
  - `foundational`: trabajo de infraestructura base, sin riesgo
  - `standard`: implementación estándar
  - `review-intensiva`: requiere revisión cuidadosa (seguridad, concurrencia, datos sensibles)
  - `critica`: alto impacto técnico, ejecución a cargo del Tech Lead
  - `ia`: requiere conocimiento de ML/AI
  - `interoperabilidad`: integración con sistemas externos

---

### Fase 1 — Historial Clínico Unificado (Semanas 1-4)

> **Objetivo:** Crear un historial clínico completo, estandarizado y compartible para cada mascota.

#### Semana 1 — Modelo de Datos Expandido + PerfilSalud

| ID | Tarea | Descripción | AC | Dificultad | Etiqueta |
|----|-------|-------------|-----|------------|----------|
| H360-01 | Migración PerfilSalud | Crear entidad `PerfilSalud` 1:1 con Mascota. Campos: pesoActual, especie, raza, color, fechaNacimiento, esterilizado, alergias(JSONB), condicionesCronicas(JSONB), historialFamiliar(JSONB). Migración Flyway. | Entidad creada. Migración ejecuta. Test JPA pasa. | M | foundational |
| H360-02 | Migración RegistroVacuna + CatalogoVacuna | Crear `RegistroVacuna` y `CatalogoVacuna`. Catálogo seed con vacunas comunes (antirrábica, moquillo, parvovirus, leucemia, etc.). Migración Flyway. | 15+ vacunas seeded. Tablas creadas. | M | foundational |
| H360-03 | Migración RegistroAlergia | Crear `RegistroAlergia` con enum severidad (LEVE, MODERADA, GRAVE). Migración Flyway. | Entidad creada. | S | foundational |
| H360-04 | Migración RegistroCirugia | Crear `RegistroCirugia`. Migración Flyway. | Entidad creada. | S | foundational |
| H360-05 | Migración RegistroAnalisis | Crear `RegistroAnalisis` con enum estado (SOLICITADO, EN_PROCESO, COMPLETADO). JSONB para resultados. Migración Flyway. | Entidad creada. | S | foundational |
| H360-06 | Expandir HistoriaClinica | Agregar campos: temperatura, frecuenciaCardiaca, frecuenciaRespiratoria, estadoHidratacion, condicionCorporal, vacunasPendientes, requiereSeguimiento, fechaProximoSeguimiento. Migración add-column. | Columnas agregadas. Backward compatible. | M | standard |

#### Semana 2 — Services y Controllers del Historial

| ID | Tarea | Descripción | AC | Dificultad | Etiqueta |
|----|-------|-------------|-----|------------|----------|
| H360-07 | ClinicalHistoryService | Service que orquesta lectura/escritura del historial completo de una mascota. Método `getCompleteHistory(mascotaId)` que retorna perfil + vacunas + alergias + cirugías + análisis + historiales clínicos. | Test unitario + integración. | M | standard |
| H360-08 | VaccinationService | CRUD de vacunas: registrarVacuna, getVacunasMascota, getProximosRefuerzos, marcarRefuerzoAplicado. Incluir lógica de "próximo refuerzo" automático. | Test: registrar vacuna → aparece en próximos refuerzos. | M | standard |
| H360-09 | AllergyService | CRUD de alergias: registrarAlergia, getAlergiasMascota, desactivarAlergia. | Test CRUD. | S | standard |
| H360-10 | ClinicalHistoryController | Endpoints: GET /api/v1/clinical-history/{mascotaId} (completo), POST /api/v1/clinical-history/vaccines, GET /api/v1/clinical-history/{mascotaId}/vaccines, POST /api/v1/clinical-history/allergies, GET /api/v1/clinical-history/{mascotaId}/allergies. | Endpoints funcionan. Swagger documentado. | M | standard |
| H360-11 | VaccinationController | Endpoints específicos de vacunación con validación de permisos (solo veterinario puede registrar). | Veterinario puede registrar, cliente solo ve. | M | standard |
| H360-12 | Seed initial vaccine catalog | Script Flyway con 20+ vacunas comunes para Perú (antirrábica, moquillo, parvovirus, leucemia, rabia, traumáticos, etc.) | Catálogo poblado. | S | foundational |

#### Semana 3 — Consentimiento y Compartir Historial

| ID | Tarea | Descripción | AC | Dificultad | Etiqueta |
|----|-------|-------------|-----|------------|----------|
| H360-13 | Consentimiento entity + migración | Crear `Consentimiento` con: mascota_id, duenio_id, destinatario_tipo (VETERINARIO/LABORATORIO/MUNICIPALIDAD/ASEGURADORA/FARMACIA), destinatario_id, ambito (COMPLETO/ESPECIFICO/LIMITADO), campos_permitidos (JSONB), fecha_inicio, fecha_fin, revocado. | Entidad + migración. | M | review-intensiva |
| H360-14 | ConsentService | Service con: crearConsentimiento, revocarConsentimiento, validarAcceso(mascotaId, destinatarioTipo, destinatarioId, campoRequerido), getConsentimientosActivos(mascotaId). Lógica: si ambito=COMPLETO ve todo; ESPECIFICO ve solo campos permitidos; LIMITADO ve todo excepto excluidos. | Test: dueño crea consentimiento → veterinario accede → dueño revoca → veterinario no accede. | L | review-intensiva |
| H360-15 | ConsentController | Endpoints: POST /api/v1/consentimientos (dueño), GET /api/v1/consentimientos/mascota/{id} (dueño), DELETE /api/v1/consentimientos/{id} (dueño, revoca). | Endpoints funcionan. | M | standard |
| H360-16 | ConsentValidator (core) | Filter/interceptor que valida consentimiento antes de permitir acceso al historial. Se integra con ClinicalHistoryController. | Si no hay consentimiento → 403. Si hay → permite. Test de integración. | L | review-intensiva |
| H360-17 | Shared history endpoint | GET /api/v1/shared-history/{mascotaId}?token={consentToken} — Endpoint público con token de consentimiento para que veterinarios externos vean el historial (con los campos permitidos). Token es JWT con expiración. | Veterinario externo ve historial filtrado por consentimiento. | M | review-intensiva |

#### Semana 4 — Integración con Módulo Existente + Tests

| ID | Tarea | Descripción | AC | Dificultad | Etiqueta |
|----|-------|-------------|-----|------------|----------|
| H360-18 | Integrar HistoriaClinica existente | Refactorizar `medical_record.HistoriaClinicaService` para que al crear un registro también actualice el `PerfilSalud` (último peso, condiciones). Backward compatible. | Los endpoints existentes de historial clínico siguen funcionando. Nuevo clinical_history se enriquece. | M | standard |
| H360-19 | Integrar PetService con PerfilSalud | Al crear/actualizar mascota, crear/actualizar PerfilSalud automáticamente. Migrar datos existentes de mascotas a PerfilSalud. | Mascotas existentes tienen PerfilSalud poblado. | M | standard |
| H360-20 | Tests de integración Fase 1 | Tests E2E: crear mascot → crear PerfilSalud → registrar vacuna → registrar alergia → veterinario crea historial clínico → dueño crea consentimiento → veterinario externo accede → dueño revoca. | Test completo pasa. | L | review-intensiva |
| H360-21 | Documentación Swagger Fase 1 | Anotaciones @Operation en todos los controllers nuevos. Verificar que Swagger UI muestra los endpoints correctamente. | Swagger completo y funcional. | S | standard |

---

### Fase 2 — Plataforma de Datos y Analytics (Semanas 5-8)

> **Objetivo:** Crear la infraestructura de datos para agregación, análisis y dashboards epidemiológicos.

#### Semana 5 — Data Lake + Tabla de Hechos

| ID | Tarea | Descripción | AC | Dificultad | Etiqueta |
|----|-------|-------------|-----|------------|----------|
| H360-22 | HechoSaludAnimal entity + migración | Tabla de hechos para analytics. Campos: fecha, mascota_id, especie, raza, edad_anos, sexo, distrito, region, enfermedad_diagnosticada, tipo_tratamiento, costo, vacuna_aplicada, empresa_id, veterinario_id. Migración. | Tabla creada. | M | foundational |
| H360-23 | ETL Service | Servicio que extrae datos de las tablas de negocio y los carga en la tabla de hechos. Ejecuta periódicamente (cada noche) o on-demand. Convierte datos normalizados en hechos analíticos. | Al ejecutar ETL, tabla de hechos se llena con datos de los últimos 30 días. | L | standard |
| H360-24 | FactTableUpdater listener | `@TransactionalEventListener` que escucha eventos de negocio (OrderPaidEvent, etc.) y actualiza la tabla de hechos en near-real-time. | Cuando se crea una historia clínica → hecho se registra. | M | standard |
| H360-25 | DataLakeConfig | Configuración para conexiones de analytics. En prod podría apuntar a una BD de analytics separada. En dev usa la misma PG. | Config creada. | S | foundational |

#### Semana 6 — Dashboard Epidemiológico

| ID | Tarea | Descripción | AC | Dificultad | Etiqueta |
|----|-------|-------------|-----|------------|----------|
| H360-26 | MetricasZona entity + migración | Tabla de métricas pre-agregadas por zona: distrito, región, periodo, total_mascotas, total_consultas, enfermedades_top[], vacunacion_pct, etc. | Tabla creada. | M | foundational |
| H360-27 | EpidemiologicalService | Service con: getMetricasPorZona(distrito, fechaInicio, fechaFin), getTendenciasEnfermedad(enfermedad, region, meses), getMapaCalor(region, enfermedad), getTopEnfermedades(distrito, periodo). Usa HechoSaludAnimal para calcular. | Tests con datos mock: "En Surco, moquillo subió 30% último mes". | L | standard |
| H360-28 | EpidemiologicalController | Endpoints: GET /api/v1/epidemiology/metrics?distrito=&fechaInicio=&fechaFin=, GET /api/v1/epidemiology/trends?enfermedad=&region=&meses=, GET /api/v1/epidemiology/heatmap?region=&enfermedad=, GET /api/v1/epidemiology/top-diseases?distrito=&periodo= | Endpoints retornan datos correctos. Swagger documentado. | M | standard |
| H360-29 | Dashboard epidemiológico integrado | Agregar al DashboardController existente: métricas epidemiológicas para EMPRESA (sus propias mascotas) y para ADMIN (vista global). | Dashboard muestra tendencias. | M | standard |
| H360-30 | Reporte PDF epidemiológico | Endpoint que genera PDF con gráficas de tendencias (usar JasperReports o PDFBox). GET /api/v1/epidemiology/report?region=&periodo= | PDF generado con gráficas. | L | standard |

#### Semana 7 — Alertas Automáticas

| ID | Tarea | Descripción | AC | Dificultad | Etiqueta |
|----|-------|-------------|-----|------------|----------|
| H360-31 | AlertaIA entity + migración | Tabla de alertas generadas por IA. Tipos: VACUNA, SEGUIMIENTO, PESO, ENFERMEDAD, BROTE, PREVENTIVA. Severidad: INFO, WARNING, CRITICAL. | Tabla creada. | M | foundational |
| H360-32 | AlertService | Service con: generarAlertasVacuna(mascotaId), generarAlertasPeso(mascotaId), generarAlertasSeguimiento(mascotaId), getAlertasUsuario(usuarioId), marcarLeida(alertaId), getAlertasPendientes(usuarioId). | Test: mascota sin vacuna en 13 meses → alerta WARNING generada. | M | standard |
| H360-33 | AlertScheduler | `@Scheduled` que diariamente revisa: (1) mascotas con vacunas próximas a vencer, (2) mascotas con peso que varía >20%, (3) seguimientos pendientes. Genera alertas automáticamente. | Al ejecutar scheduler → alertas aparecen para mascotas elegibles. | M | standard |
| H360-34 | AlertController | Endpoints: GET /api/v1/alerts (mis alertas), PATCH /api/v1/alerts/{id}/read, GET /api/v1/alerts/count (no leídas). | Endpoints funcionan. | S | standard |
| H360-35 | Integración email para alertas críticas | Cuando severidad=CRITICAL → enviar email al dueño con la alerta. Usar EmailService existente. | Email enviado para alertas críticas. | M | standard |

#### Semana 8 — Tests y Hardening Fase 2

| ID | Tarea | Descripción | AC | Dificultad | Etiqueta |
|----|-------|-------------|-----|------------|----------|
| H360-36 | Tests E2E Fase 2 | Test completo: crear mascota → registrar historia clínica → ETL ejecuta → dashboard muestra métricas → scheduler genera alerta → email enviado. | Test E2E pasa. | L | review-intensiva |
| H360-37 | Optimización queries analytics | Indexar HechoSaludAnimal para queries por fecha+distrito+enfermedad. Analizar EXPLAIN de queries principales. | Queries < 200ms con 100K registros. | M | review-intensiva |
| H360-38 | Documentación Swagger Fase 2 | Anotaciones @Operation en controllers nuevos. | Swagger completo. | S | standard |

---

### Fase 3 — IA y Predicción (Semanas 9-14)

> **Objetivo:** Implementar IA completa: chatbot RAG, predicción de enfermedades, análisis de imágenes.

#### Semana 9-10 — Chatbot RAG

| ID | Tarea | Descripción | AC | Dificultad | Etiqueta |
|----|-------|-------------|-----|------------|----------|
| H360-39 | Knowledge base preparation | Preparar base de conocimiento veterinaria: enfermedades comunes en Perú, tratamientos, vacunas, dosis, emergencias. Formato: documentos chunked para RAG. Almacenar en tabla PostgreSQL (no vector DB por ahora). | 100+ documentos de conocimiento veterinario almacenados. | L | ia |
| H360-40 | ChatbotRAGService | Service que: (1) recibe pregunta del usuario, (2) busca contexto relevante en knowledge base (similarity search simple con embeddings pre-computados o TF-IDF), (3) busca historial de la mascota si se menciona, (4) construye prompt con contexto + historial, (5) llama a OpenAI, (6) retorna respuesta con fuentes. | Test: "¿Cada cuánto se vacuna un cachorro de moquillo?" → respuesta correcta con fuentes. | XL | ia |
| H360-41 | ChatbotController | Endpoints: POST /api/v1/ia/chat (message, mascotaId opcional), GET /api/v1/ia/chat/history (historial de conversación). Rate limit: 20 mensajes/hora por usuario (usar suscripción limitIaUso). | Chatbot responde correctamente. Rate limit funciona. | M | ia |
| H360-42 | Chat history entity | Almacenar conversaciones: usuario_id, mascota_id, mensaje_usuario, respuesta_ia, timestamp. Para mejorar el chatbot con feedback. | Tabla + service. | M | standard |

#### Semana 11-12 — Predicción de Enfermedades

| ID | Tarea | Descripción | AC | Dificultad | Etiqueta |
|----|-------|-------------|-----|------------|----------|
| H360-43 | PredictiveService | Service que usando datos de HechoSaludAnimal + perfil de la mascota predice: (1) Probabilidad de enfermedades comunes por especie/raza/edad/zona, (2) Riesgo de complicaciones, (3) Necesidad de vacunación. Usa OpenAI con prompt especializado + datos históricos. | Test: perroLabrador 8 años en Lima → predice riesgo de displasia de cadera. | XL | ia |
| H360-44 | PrediccionEnfermedad entity | Almacenar predicciones: mascota_id, enfermedad, probabilidad, nivel_riesgo, recomendaciones, fecha, modelo_usado. | Entidad + migración. | M | standard |
| H360-45 | PredictionController | Endpoints: POST /api/v1/ia/predict/{mascotaId} (genera predicción), GET /api/v1/ia/predictions/{mascotaId} (historial de predicciones). | Endpoints funcionan. | M | ia |
| H360-46 | PredictionScheduler | Job semanal que genera predicciones proactivas para mascotas con perfil completo (edad > 5 años, múltiples registros clínicos). | Al ejecutar → predicciones generadas para mascotas elegibles. | M | ia |

#### Semana 13-14 — Análisis de Imágenes + Alertas Epidemiológicas IA

| ID | Tarea | Descripción | AC | Dificultad | Etiqueta |
|----|-------|-------------|-----|------------|----------|
| H360-47 | ImageAnalysisService | Service que: (1) recibe imagen de lesión/piel/mucosa, (2) envía a OpenAI Vision (gpt-4o), (3) retorna: tipo de lesión probable, nivel de urgencia, recomendación, si requiere visita al veterinario. Prompt especializado en dermatología veterinaria. | Test con imagen de ejemplo → análisis retornado. | XL | ia |
| H360-48 | AnalisisImagen entity | Almacenar: mascota_id, imagen_url, resultado_json, nivel_urgencia, recomendacion, veterinario_validador_id (nullable), fecha. | Entidad + migración. | M | standard |
| H360-49 | ImageAnalysisController | Endpoints: POST /api/v1/ia/analyze-image (multipart, mascotaId), GET /api/v1/ia/image-analyses/{mascotaId}. | Endpoint funciona. Imagen procesada. | M | ia |
| H360-50 | EpidemiologicalAlertService (IA) | Service que analiza HechoSaludAnimal para detectar brotes: (1) Compara incidencia actual con promedio histórico, (2) Si supera umbral → genera AlertaEpidemiologica, (3) Notifica a municipalidades y veterinarios de la zona. | Test: incremento del 300% en diarrea viral → alerta generada. | L | ia |
| H360-51 | AlertaEpidemiologica entity + migración | Tabla de alertas epidemiológicas con: enfermedad, region, distrito, periodo, casos, umbral_superado, nivel_riesgo, notificado_a(JSONB), resuelto. | Entidad + migración. | M | foundational |
| H360-52 | EpidemiologicalAlertScheduler | Job diario que ejecuta la detección de brotes para todas las regiones activas. | Al ejecutar → brotes detectados y notificados. | M | ia |

---

### Fase 4 — Ecosistema Conectado (Semanas 15-22)

> **Objetivo:** Integrar laboratorios, municipalidades, farmacias y aseguradoras.

#### Semana 15-16 — Laboratorios

| ID | Tarea | Descripción | AC | Dificultad | Etiqueta |
|----|-------|-------------|-----|------------|----------|
| H360-53 | Laboratorio entity + migración | Crear entidad con: nombre, ruc, direccion, telefono, email, api_key (hash), endpoint_callback, activo, verified. Migración Flyway. | Entidad + migración. | M | foundational |
| H360-54 | ApiKeyAuthFilter | Filter para autenticación de sistemas externos via API key (Header: X-API-Key). Hashea y compara con BD. Rate limit por API key. | Filter funciona: API key válida → pasa; inválida → 401. | L | review-intensiva |
| H360-55 | LabService | Service con: registerLab, getLabs, ordenarAnalisis(mascotaId, labId, tipoAnalisis), recibirResultados(labId, ordenId, resultadosJson), getAnalisisMascota(mascotaId). | Test: veterinario ordena → lab recibe → resultados se guardan en historial. | L | interoperabilidad |
| H360-56 | LabController + LabPublicController | Privado: POST /api/v1/labs (admin), GET /api/v1/labs, POST /api/v1/labs/{labId}/orders (veterinario). Público (API key): POST /api/v1/public/labs/{labId}/results/{ordenId} (lab envía resultados). | Endpoints funcionan. | M | interoperabilidad |
| H360-57 | OrdenAnalisis entity | Crear entidad que vincula: mascota_id, lab_id, tipo_analisis, fecha_solicitud, fecha_resultado, resultado_json, archivo_url, veterinario_solicitante_id, estado. | Entidad + migración. | M | foundational |
| H360-58 | Integración con RegistroAnalisis existente | Cuando lab envía resultados → se actualiza RegistroAnalisis existente + se crea HistoriaClinica con los resultados + se notifica al veterinario y dueño. | Flujo completo funciona. | M | interoperabilidad |

#### Semana 17-18 — Municipalidades

| ID | Tarea | Descripción | AC | Dificultad | Etiqueta |
|----|-------|-------------|-----|------------|----------|
| H360-59 | Municipalidad entity + migración | Crear entidad con: nombre, codigo_municipalidad, region, distrito, contacto_email, endpoint_callback, activo. Migración. | Entidad + migración. | M | foundational |
| H360-60 | MunicipalityService | Service con: registerMunicipality, getMunicipalities, getMascotasPorDistrito(distrito), getEstadisticasDistrito(distrito, periodo), enviarAlerta(municipalidadId, alerta), getAlertasRecibidas(municipalidadId). | Test: consultar estadísticas del distrito → retorna datos correctos. | L | interoperabilidad |
| H360-61 | MunicipalityController | Endpoints: POST /api/v1/municipalities (admin), GET /api/v1/municipalities, GET /api/v1/municipalities/{id}/stats, GET /api/v1/municipalities/{id}/alerts. POST endpoint para recibir alertas epidemiológicas. | Endpoints funcionan. | M | interoperabilidad |
| H360-62 | Integración con AlertaEpidemiologica | Cuando se genera AlertaEpidemiologica → automáticamente se envía a municipalidades del distrito afectado (via callback o email). | Alerta generada → municipalidad notificada. | M | interoperabilidad |
| H360-63 | Dashboard municipal | Endpoint que retorna: total mascotas registradas, vacunación %, enfermedades más comunes, tendencias. GET /api/v1/municipalities/{id}/dashboard | Dashboard municipal funcional. | M | standard |

#### Semana 19-20 — Farmacias + Aseguradoras

| ID | Tarea | Descripción | AC | Dificultad | Etiqueta |
|----|-------|-------------|-----|------------|----------|
| H360-64 | Farmacia entity + migración | Crear entidad con: empresa_id (FK nullable), nombre, ruc, direccion, telefono, api_key, activo. Migración. | Entidad + migración. | M | foundational |
| H360-65 | PharmacyService | Service con: registerPharmacy, getPharmacies, dispensarMedicamento(farmaciaId, recetaId, medicamento), getHistorialDispensacion(mascotaId). | Test: receta generada → farmacia dispensa → historial actualizado. | L | interoperabilidad |
| H360-66 | PharmacyController | Endpoints: POST /api/v1/pharmacies (admin), GET /api/v1/pharmacies, POST /api/v1/public/pharmacies/{id}/dispense (API key). | Endpoints funcionan. | M | interoperabilidad |
| H360-67 | Aseguradora entity + migración | Crear entidad con: nombre, ruc, endpoint_callback, api_key, activo. Migración. | Entidad + migración. | M | foundational |
| H360-68 | InsuranceService | Service con: registerInsurance, getInsurances, verificarCobertura(aseguradoraId, mascotaId, tratamiento), registrarReclamo(aseguradoraId, mascotaId, tratamiento, monto), getHistorialReclamos(mascotaId). | Test: verificar cobertura → respuesta correcta. | L | interoperabilidad |
| H360-69 | InsuranceController | Endpoints: POST /api/v1/insurances (admin), GET /api/v1/insurances, POST /api/v1/public/insurances/{id}/verify (API key), POST /api/v1/public/insurances/{id}/claims (API key). | Endpoints funcionan. | M | interoperabilidad |

#### Semana 21-22 — API Gateway + Documentación + Tests

| ID | Tarea | Descripción | AC | Dificultad | Etiqueta |
|----|-------|-------------|-----|------------|----------|
| H360-70 | InteroperabilityGateway | Service central que orquesta todos los intercambios de datos. Registra cada intercambio en IntercambioHL7 para auditoría. Valida permisos antes de cada intercambio. | Todos los intercambios pasan por el gateway. | L | review-intensiva |
| H360-71 | IntercambioHL7 entity + migración | Tabla de auditoría de intercambios: origen_tipo, origen_id, destino_tipo, destino_id, tipo_dato, formato, payload_hash, exitoso, error_mensaje. | Tabla creada. | M | foundational |
| H360-72 | API pública documentada | OpenAPI spec completa para sistemas externos. Incluir: autenticación (API key), rate limits, formatos de datos, ejemplos de request/response. | Documentación Swagger completa para partners. | L | standard |
| H360-73 | Tests E2E Fase 4 | Test completo: laboratorio se registra → veterinario ordena analisis → lab recibe → resultados llegan → historial se actualiza → municipalidad recibe alerta epidemiológica → farmacia dispensa medicamento → aseguradora verifica cobertura. | Test E2E completo pasa. | XL | review-intensiva |
| H360-74 | Rate limiting por API key | Bucket4j configurado para endpoints de API key: 100 req/min por laboratorio, 50 req/min por municipalidad. | Rate limits funcionan. | M | standard |
| H360-75 | Monitoring y health checks | Actuator endpoints para cada integración: /actuator/health/labs, /actuator/health/municipalities, etc. | Health checks funcionan. | S | standard |

---

### Fase 5 — Hardening + Optimización (Semanas 23-26)

> **Objetivo:** Production-ready con tests completos, performance, seguridad.

#### Semana 23-24 — Tests y Cobertura

| ID | Tarea | Descripción | AC | Dificultad | Etiqueta |
|----|-------|-------------|-----|------------|----------|
| H360-76 | Unit tests Fase 1-4 | Tests unitarios para: ClinicalHistoryService, VaccinationService, ConsentService, AlertService, PredictiveService, ChatbotRAGService, LabService, MunicipalityService, PharmacyService, InsuranceService. | Cobertura > 70% en módulos nuevos. | XL | standard |
| H360-77 | Integration tests Fase 1-4 | Tests de integración con Testcontainers para: flujos de consentimiento, sharing de historial, intercambio con lab, alertas epidemiológicas. | Tests pasan en CI. | XL | review-intensiva |
| H360-78 | Security audit | Revisar: autenticación API key, autorización de consentimiento, rate limiting, input validation en todos los endpoints nuevos. | No findings de seguridad. | L | review-intensiva |

#### Semana 25-26 — Performance + Deploy

| ID | Tarea | Descripción | AC | Dificultad | Etiqueta |
|----|-------|-------------|-----|------------|----------|
| H360-79 | Database optimization | Índices en: HechoSaludAnimal, Consentimiento, RegistroVacuna, AlertaIA. Analyze EXPLAIN de queries principales. | Todas las queries < 200ms. | M | review-intensiva |
| H360-80 | Cache strategy | Implementar cache para: catálogo de vacunas (30min), métricas epidemiológicas (1h), perfil de salud (5min). Usar Redis existente. | Cache funciona. Hit rate > 80%. | M | standard |
| H360-81 | Async processing | Mover a @Async: ETL de data lake, generación de alertas epidemiológicas, análisis de imágenes. Usar thread pools existentes (AsyncConfig). | Procesamiento async funciona. | M | standard |
| H360-82 | Production config | Crear application-prod.yaml con configuración para: API keys de laboratorios, URLs de municipalidades, límites de IA, cache TTLs producción. | Configuración prod completa. | S | standard |
| H360-83 | README actualizado | Actualizar README.md con: nueva arquitectura, endpoints de interoperabilidad, guía de integración para partners, variables de entorno necesarias. | README completo. | S | standard |
| H360-84 | Deployment verification | Verificar que la aplicación levanta con todas las nuevas migraciones, que los endpoints existentes no se rompieron, que los nuevos endpoints responden correctamente. | Smoke test completo pasa. | M | standard |

---

## 8. Librerías y Dependencias Nuevas

| Dependencia | Versión | Propósito |
|-------------|---------|-----------|
| org.springframework.ai:spring-ai-openai-spring-boot-starter | 1.0.0 | OpenAI integration (reemplaza RestTemplate manual) |
| org.springframework.ai:spring-ai-pgvector-store-spring-boot-starter | 1.0.0 | Vector embeddings para RAG (opcional, puede ser futuro) |
| com.google.zxing:core + javase | 3.5.3 | QR generation (ya en MeEvent, verificar para Huella360) |
| org.apache.commons:commons-text | 1.12.0 | Text similarity para knowledge base |
| org.apache.commons:commons-math3 | 3.6.1 | Statistical functions para epidemiología |
| com.opencsv:opencsv | 5.9 | Export CSV de reportes |
| net.logstash.logback:logstash-logback-encoder | 7.4 | Logs JSON estructurados |
| io.micrometer:micrometer-registry-prometheus | (via Spring Boot) | Métricas para monitoring |

**No agregar (ya disponible):**
- Spring Boot Starter Web, Validation, Security, Data JPA, Cache, WebSocket, Mail, Actuator
- Bucket4j (rate limiting)
- SpringDoc/Swagger (API docs)
- Testcontainers (test integración)
- Lombok, MapStruct

---

## 9. Riesgos y Decisiones Abiertas

### Para conversar con el producto antes/durante Fase 1:

1. **¿Qué datos son obligatorios vs opcionales en el historial clínico?** Definir schema mínimo viable.
2. **¿Quién puede ver qué?** Matriz de permisos: veterinario propio vs externo vs municipalidad vs aseguradora.
3. **¿Consentimiento por defecto o explícito?** ¿Al registrar una mascota se autoriza a todos los veterinarios o solo a los que el dueño elige?
4. **¿Los laboratorios se conectan via API o via portal web?** Define la complejidad de la integración.
5. **¿Municipalidades requieren datos en tiempo real o reportes periódicos?** Define la frecuencia de ETL.
6. **¿La IA usa OpenAI o modelo local?** OpenAI tiene costo por token; modelo local requiere infraestructura.
7. **¿Historial clínico es transferible cuando la mascota cambia de dueño?** Definir ownership de datos.
8. **¿Los datos del data lake son anonimizados para análisis público?** Temas de privacidad.

### Riesgos Técnicos:

| Riesgo | Impacto | Mitigación |
|--------|---------|------------|
| Consentimiento mal diseñado | Alto — datos sensibles expuestos | Revisión exhaustiva de seguridad, tests de autorización |
| Performance del data lake con mucho volumen | Medio — queries lentas | Índices correctos, particionamiento por fecha |
| Costos de OpenAI para chatbot/predicción | Medio — costos crecientes | Rate limits por usuario, caching de respuestas, modelo local futuro |
| Integración con lab rechazada por ellos | Bajo — no adoptan la API | Portal web como alternativa a la API |
| RGPD/Ley de protección de datos | Alto — multas | Consentimiento explícito, anonimización, derecho al olvido |
| Complejidad de HL7 FHIR | Medio — implementación pesada | Empezar con JSON simplificado, migrar a FHIR completo gradualmente |

### Decisiones Pendientes:

| Decisión | Opciones | Recomendación |
|----------|----------|---------------|
| Formato de intercambio | HL7 FHIR completo vs JSON simplificado | JSON simplificado → FHIR gradual |
| Vector DB para RAG | pgvector vs ChromaDB vs Weaviate | pgvector (ya usa PostgreSQL) |
| Análisis de imágenes | OpenAI Vision vs modelo fine-tuned | OpenAI Vision → modelo custom futuro |
| Almacenamiento de imágenes médicas | Cloudinary (ya existe) vs S3 | Cloudinary (ya integrado) |
| Horario de ETL | Noche (3am) vs cada hora | Noche para MVP, cada hora post-MVP |

---

## 10. Métricas de Éxito

| Métrica | Target |
|---------|--------|
| Mascotas con perfil de salud completo | > 50% en 6 meses post-lanzamiento |
| Veterinarios usando historial compartido | > 30% en 3 meses |
| Alertas de vacuna generadas correctamente | > 95% precisión |
| Tiempo de respuesta del chatbot | < 3 segundos |
| Labs conectados vía API | > 5 en 6 meses |
| Municipalidades usando dashboard | > 3 en 6 meses |
| Reducción de exámenes repetidos (reportado) | > 20% |
| Uptime de APIs de interoperabilidad | > 99.5% |

---

## 11. Glossario

| Término | Definición |
|---------|-----------|
| PerfilSalud | Entidad que consolida la información de salud de una mascota |
| Consentimiento | Autorización del dueño para compartir datos con terceros |
| HechoSaludAnimal | Registro en tabla de hechos para analytics |
| AlertaIA | Notificación generada por IA (vacuna, peso, enfermedad, brote) |
| AlertaEpidemiologica | Alerta generada cuando se detecta un posible brote |
| InteroperabilityGateway | Capa central que gestiona intercambios de datos con sistemas externos |
| RAG | Retrieval-Augmented Generation — IA que usa datos contextualizados |
| HL7 FHIR | Estándar de intercambio de datos de salud (simplificado para este proyecto) |
| ETL | Extract, Transform, Load — proceso de carga de datos al data lake |
| Data Lake | Repositorio centralizado de datos para análisis |
| API Key | Clave de autenticación para sistemas externos |
| IntercambioHL7 | Registro de auditoría de cada intercambio de datos |

---

*Documento generado el 2026-07-27. Versión 1.0.*
