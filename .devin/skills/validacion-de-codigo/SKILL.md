---
name: validacion-de-codigo
description: "Converted from Devin playbook: Validacion de Codigo"
triggers:
  - user
  - model
---

# Validacion de Codigo

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Validacion de Codigo (playbook-432d7603befe4440af4cf7dbff078a20), macro `!validacion`

## Procedure

# Playbook: Validación Sintáctica, Semántica, Seguridad y Estilo — Devin Playbook

## Overview
Flujo determinista para validar **sintaxis**, **semántica**, **seguridad** y **estilo** en un Pull Request de GitHub.  
Devin realiza revisión estática (solo lectura), genera comentarios anclados por línea, una decisión general y un PR documental con el resumen de validación.

---

## 1. Datos y Entradas
- **Requiere**: URL/número de PR, acceso de lectura al repo.  
- **Documentos**: descripción del PR, guías de estilo, reglas de seguridad/linter.  
- **Herramientas**: `gh`, `git` (read-only), linters/analizadores estáticos opcionales.  

---

## 2. Objetivo y Reglas
**Objetivo:** Validar el PR en cuatro áreas:
1. **Sintaxis:** Corrección del lenguaje y formato.
2. **Semántica:** Lógica, tipos y coherencia.
3. **Seguridad:** Validaciones, secretos, inyecciones, errores seguros.
4. **Estilo:** Convenciones, legibilidad y consistencia.

**Reglas:**
- No ejecutar código ni tests.  
- Referenciar líneas y justificar cada observación.  
- Tono profesional, claro y accionable.  
- Basarse en guías y linters del proyecto.  

---

## 3. Ejemplos de Comentarios

**[file: src/auth/middleware.ts | line 27]**  
**[blocker]** `req.user` usado sin validación previa.  
**Por qué:** Riesgo de acceso no autorizado.  
**Sugerencia:**
```ts
if (!req.user) return res.status(401).send("Unauthorized");
````

**[file: utils/math.py | line 88]**
**[major]** Comparación de floats con `==`.
**Por qué:** Errores de precisión.
**Sugerencia:**

```python
if math.isclose(a, b, rel_tol=1e-9):
```

---

## 4. Plantilla de Revisión General

```md
## Resumen
- Alcance: <una línea>
- Riesgo: <Bajo|Medio|Alto>
- Positivos: <puntos clave>
- Observaciones: <lista de problemas>
## Decisión
<Approve | Request changes | Comment>
## Próximos pasos
<lista de acciones>
```

---

## 5. Severidad de Comentarios

| Nivel         | Significado                           |
| ------------- | ------------------------------------- |
| **[blocker]** | Error crítico (sintaxis o seguridad)  |
| **[major]**   | Problema funcional o de mantenimiento |
| **[minor]**   | Mejora recomendable                   |
| **[nit]**     | Estilo o formato                      |

---

## 6. Checklist de Validación

**Sintaxis:** sin errores de parseo, formato correcto, lint limpio.
**Semántica:** tipos correctos, lógica coherente, sin variables no usadas.
**Seguridad:** inputs validados, sin secretos, sin inyecciones.
**Estilo:** nombres consistentes, comentarios claros, sin código muerto.

---

## 7. Procedimiento

```bash
# Preparar entorno
gh auth status
gh pr view <PR> --json title,number,author,body

# Revisar PR (solo lectura)
gh pr checkout <PR>
gh pr diff <PR> --patch

# Comentar línea
gh pr review <PR> \
  --comment \
  --body "**[major]** Falta validación de input (línea 48)." \
  --path src/auth/controller.ts --line 48

# Enviar resumen general
gh pr review <PR> --request-changes --body "$(cat review_summary.md)"
```

---

## 8. PR de Documentación

```bash
git switch -c devin/validation-pr-<PRNUM>
mkdir -p docs/validations
cat > docs/validations/<PRNUM>.md <<'EOF'
# Validación PR <PRNUM>
<paste resumen general aquí>
EOF
git add docs/validations/<PRNUM>.md
git commit -m "docs(validation): resumen de validación para PR #<PRNUM>"
git push -u origin devin/validation-pr-<PRNUM>
gh pr create --fill --title "Docs: Validación PR #<PRNUM>" --body "Resumen de validación sintáctica, semántica, seguridad y estilo."
```

---

## 9. Criterios de Finalización

* Comentarios con severidad y sugerencias.
* Decisión general enviada.
* PR documental creado (`devin/validation-pr-<PRNUM>`).

---

## 10. Prohibido

* Modificar código del PR.
* Ejecutar tests o el sistema.
* Exponer secretos o código propietario.

---

## 11. Consejos

* Priorizar seguridad y sintaxis antes del estilo.
* Referenciar reglas del proyecto (lint, seguridad).
* Mantener feedback breve, claro y accionable.

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
