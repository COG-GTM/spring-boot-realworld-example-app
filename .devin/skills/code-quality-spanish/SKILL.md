---
name: code-quality-spanish
description: "Converted from Devin playbook: Code Quality - Spanish"
triggers:
  - user
  - model
---

# Code Quality - Spanish

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Code Quality - Spanish (playbook-7f6ba81fae6345f0b2c3966ef49b374d), macro `!code_quality`

## Procedure

# **Playbook: Continuous Code Quality Gate — Devin Playbook**

### **Overview**

Flujo determinista para establecer un **Quality Gate automático previo al merge**.
Devin ejecuta validaciones de calidad y seguridad en cada Pull Request mediante análisis estático y reglas configuradas (lint, seguridad, convenciones, cobertura).
El resultado es un reporte y una decisión de *“pasar o bloquear”* el merge, acompañada de un PR documental con métricas y observaciones.

---

## **1. Datos y Entradas**

**Requiere:**

* URL o número de PR en GitHub.
* Acceso de lectura al repositorio.
* Configuración de reglas de calidad y seguridad (`.eslintrc`, `.sonar-project.properties`, `bandit.yml`, etc.).

**Documentos:**

* Guía de estándares de código del proyecto.
* Política de seguridad y criterios de aprobación.
* Métricas de cobertura o análisis previos.

**Herramientas:**

* `gh`, `git` (solo lectura).
* Analizadores: ESLint / SonarQube / Bandit / Semgrep / Pylint / Checkstyle, según lenguaje.
* Scripts de validación local o CI.

---

## **2. Objetivo y Reglas**

**Objetivo:**
Garantizar que ningún PR se fusione si viola reglas de calidad o seguridad definidas por la organización.

**Áreas de Control:**

1. **Calidad del código:** errores de lint, deuda técnica, duplicación, cobertura mínima.
2. **Seguridad:** detección de patrones inseguros, secretos, dependencias vulnerables.
3. **Convenciones:** naming, estilo, estructura, documentación mínima.
4. **Integridad:** sin archivos generados, binarios o configuraciones locales.

**Reglas:**

* No modificar código ni ejecutar lógica de negocio.
* Solo revisión estática y análisis determinista.
* Resultado binario: **Pass** o **Fail**, con justificación.
* Severidad por categorías: blocker / major / minor / info.
* Todo hallazgo debe tener referencia de archivo y línea.

---

## **3. Ejemplos de Hallazgos**

```text
[file: src/core/utils.py | line 54]
[blocker] Uso de eval() detectado.
Por qué: ejecución arbitraria. Sustituir por parse seguro o lista blanca.

[file: src/components/Button.tsx | line 87]
[major] Falta test unitario asociado al nuevo componente.
Por qué: cobertura mínima 80% requerida por política.

[file: package.json | line 14]
[minor] Dependencia obsoleta: lodash 4.17.20.
Sugerencia: actualizar a 4.17.21 o posterior.

[file: src/api/routes.js | line 112]
[info] Comentario pendiente: // TODO refactorizar.
Sugerencia: crear ticket técnico.
```

---

## **4. Plantilla de Resumen General**

```markdown
## Resumen de Quality Gate
- Alcance: Validación automática previa a merge del PR #<PRNUM>
- Riesgo: <Bajo | Medio | Alto>
- Resultados:
  - Lint: <ok / errores>
  - Seguridad: <ok / vulnerabilidades>
  - Cobertura: <x% vs mínimo y%>
- Positivos: <lista>
- Observaciones: <resumen de problemas>

## Decisión
<✅ PASS | ❌ FAIL>

## Acciones Requeridas
- [ ] Corregir errores bloqueantes.
- [ ] Revisar vulnerabilidades.
- [ ] Confirmar cobertura mínima.
```

---

## **5. Severidad de Hallazgos**

| Nivel         | Significado                                 |
| ------------- | ------------------------------------------- |
| **[blocker]** | Rompe políticas o seguridad. Bloquea merge. |
| **[major]**   | Problema funcional o de mantenibilidad.     |
| **[minor]**   | Mejora recomendable o warning.              |
| **[info]**    | Nota informativa o sugerencia.              |

---

## **6. Checklist del Quality Gate**

**Código:**

* [ ] Linter sin errores.
* [ ] No hay duplicación >5%.
* [ ] Sin variables no usadas ni funciones vacías.

**Seguridad:**

* [ ] Sin uso de `eval`, `exec`, ni comandos shell inseguros.
* [ ] Sin secretos (API keys, passwords).
* [ ] Dependencias sin CVEs abiertas.

**Estilo y Convenciones:**

* [ ] Nombres consistentes y descriptivos.
* [ ] Comentarios útiles y actualizados.
* [ ] Sin código muerto o comentado.

**Integridad:**

* [ ] No incluye archivos compilados o locales (`.env`, `.DS_Store`).
* [ ] Documentación o README actualizados si aplica.

---

## **7. Procedimiento**

```bash
# 1. Preparar entorno
gh auth status
gh pr view <PRNUM> --json title,number,author,body

# 2. Clonar y revisar PR
gh pr checkout <PRNUM>

# 3. Ejecutar Quality Gate local o CI
npm run lint
bandit -r src/
sonar-scanner -Dsonar.projectKey=myapp -Dsonar.qualitygate.wait=true

# 4. Generar reporte
mkdir -p reports/quality
cp sonar-report.json reports/quality/
cp lint-report.txt reports/quality/

# 5. Crear resumen
cat > review_summary.md <<'EOF'
## Resumen de Quality Gate PR #<PRNUM>
Lint: 2 errores, 3 warnings.
Seguridad: 0 vulnerabilidades.
Cobertura: 83% (mínimo 80%).
Decisión: PASS
EOF

# 6. Comentar en PR
gh pr review <PRNUM> --comment --body "Quality Gate ejecutado: PASS. Reporte completo en docs/quality/pr-<PRNUM>.md"

# 7. Crear PR documental
git switch -c devin/quality-gate-<PRNUM>
mkdir -p docs/quality
cp review_summary.md docs/quality/pr-<PRNUM>.md
git add docs/quality/pr-<PRNUM>.md
git commit -m "docs(quality): resumen de quality gate para PR #<PRNUM>"
git push -u origin devin/quality-gate-<PRNUM>
gh pr create --fill --title "Docs: Quality Gate PR #<PRNUM>" --body "Resumen de validación de calidad y seguridad."
```

---

## **8. Criterios de Finalización**

* Reporte de análisis generado y almacenado.
* Comentarios anclados con severidad y sugerencias.
* Decisión final registrada (**PASS / FAIL**).
* PR documental creado (`devin/quality-gate-<PRNUM>`).

---

## **9. Prohibido**

* Ejecutar código del proyecto o pruebas con side effects.
* Modificar código fuente del PR.
* Exponer datos sensibles o tokens.
* Aceptar un PR con [blocker] sin corrección.

---

## **10. Consejos**

* Priorizar vulnerabilidades y deuda técnica antes del estilo.
* Referenciar las métricas de Sonar o linters específicos.
* Mantener reportes legibles y breves.
* Configurar el Quality Gate en CI para ejecución automática.
* Escalar PRs con fallos de seguridad críticos.

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
