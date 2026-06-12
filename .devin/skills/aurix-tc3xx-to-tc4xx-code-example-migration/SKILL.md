---
name: aurix-tc3xx-to-tc4xx-code-example-migration
description: "Converted from Devin playbook: AURIX TC3xx to TC4xx Code Example Migration"
triggers:
  - user
  - model
---

# AURIX TC3xx to TC4xx Code Example Migration

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: AURIX TC3xx to TC4xx Code Example Migration (playbook-66f5b31caf434c9ea017e781e968b316), macro `!aurix`

## Procedure

## Overview

This playbook guides the migration of AURIX TC3xx (e.g., TC375, TC387, TC397) code examples to TC4xx (e.g., TC4D7, TC4D9, TC499, TC489). TC3xx examples use iLLD v1.x (library prefix `iLLD_1_*`), while TC4xx examples use iLLD v2.x (library prefix `iLLD_2_*`). The migration involves infrastructure changes (project config, linker scripts, watchdog API, clock config) and peripheral API changes (GTM->EGTM, EVADC->TMADC, ScuWdt->Wtu, etc.).

**Repository:** COG-GTM/AURIX_code_examples

**Strategy:** Always start from the closest existing TC4xx template example and port the application logic into it. Do NOT try to modify a TC3xx project in-place -- the infrastructure differences are too large.

## What's Needed From User

- The TC3xx source example to migrate (e.g., `code_examples/iLLD_TC387_ADS_GTM_TOM_3_Phase_Inverter_PWM_2/`)
- The target TC4xx device (default: TC4D7 / KIT_A3G_TC4D7_LITE if not specified)
- Any specific peripheral or feature requirements

## Procedure

### STEP 1: Identify the Source and Target

1. Identify the TC3xx source example (e.g., `code_examples/iLLD_TC387_ADS_GTM_TOM_3_Phase_Inverter_PWM_2/`).
2. Identify the target TC4xx device. Default to TC4D7 (KIT_A3G_TC4D7_LITE) unless the user specifies otherwise.
3. Choose the closest existing TC4xx template to use as a base. Look in `code_examples/` for `iLLD_TC4D7_LK_ADS_*` examples. Good base templates:
   - Simple peripheral example: `iLLD_TC4D7_LK_ADS_ADC_Single_Channel/`
   - Timer/PWM example: `iLLD_TC4D7_LK_ADS_EGTM_ATOM_3_Phase_Inverter_PWM_1/`
   - OneEye example: `iLLD_TC4D7_LK_ADS_ONEEYE_DAS_QUICKSTART/`
   - Flash example: `iLLD_TC4D7_LK_ADS_DFLASH_Multi_Data_Programming/`

### STEP 2: Create the New Project Directory

1. Name the new project following TC4xx convention: `iLLD_TC4D7_LK_ADS_<FEATURE_NAME>/` (include `LK` for lite kit).
2. Copy the entire chosen TC4xx template directory as the starting point.

### STEP 3: Copy Infrastructure from TC4xx Template (DO NOT modify these from TC3xx source)

The following files should come from the TC4xx template, NOT from the TC3xx source:

- **`.ads/install-libraries.json`** -- Keep the TC4xx version as-is. It contains TC4xx device maps (TC4DA, TC49xN, TC48x, TC46x, TC45x, TC44x, TC4Zx), platform maps, and references to iLLD v2.x zips. Reference: `code_examples/iLLD_TC4D7_LK_ADS_ADC_Single_Channel/.ads/install-libraries.json`

- **`.ads/install.json`** -- Keep the TC4xx version as-is.

- **`Boards/board.h`** and **`Boards/KIT_TC4D7_LITE/`** -- Keep the TC4xx version. This defines `IFX_DEVICE_FAMILY` as `IFX_DEVICE_FAMILY_TC4` and `IFX_DEVICE_SERIES` as `IFX_DEVICE_SERIES_TC4D`. Reference: `code_examples/iLLD_TC4D7_LK_ADS_ADC_Single_Channel/Boards/board.h`

- **Linker script (`Lcf_Gcc_Tricore_Tc.lsl`)** -- Keep the TC4xx version. Key differences from TC3xx:
  - `__TRICORE_DERIVATE_MEMORY_MAP__` = `0x4D0` (not `0x380`)
  - 6 cores (CPU0-CPU5) with CSA/USTACK/ISTACK for each
  - All DSPR sizes are 240K
  - Different interrupt vector table addresses
  Reference: `code_examples/iLLD_TC4D7_LK_ADS_ADC_Single_Channel/Lcf_Gcc_Tricore_Tc.lsl`

- **`.cproject`** -- Keep the TC4xx version. CPU derivative is `tc4dax`, include paths reference `TC4DA`/`TC4xx`.

- **`.project`** -- Update the project name to match the new directory name.

- **`Cpu1_Main.c` through `Cpu5_Main.c`** -- Keep the TC4xx versions (TC4xx has 6 cores vs TC3xx's 3-4).

### STEP 4: Migrate `Configurations/Ifx_Cfg.h`

Start from the TC4xx template's `Ifx_Cfg.h` and apply these rules:

1. **Clock macros** -- Use TC4xx naming convention (NOT TC3xx):
   - `IFX_CFG_SCU_XTAL_FREQUENCY` becomes `IFX_CFG_CLOCK_XTAL_FREQUENCY` (use `BOARD_CLOCK_XTAL_HZ` if board.h is included, or `25000000` for TC4D7 lite kit)
   - `IFX_CFG_SCU_PLL_FREQUENCY` becomes `IFX_CFG_CLOCK_SYSPLL_FREQUENCY` (typically `500000000` for TC4xx)
   - `IFX_CFG_SCU_PLL1_FREQUENCY` becomes `IFX_CFG_CLOCK_PERPLL1_FREQUENCY`
   - `IFX_CFG_SCU_PLL2_FREQUENCY` becomes `IFX_CFG_CLOCK_PERPLL2_FREQUENCY`
   - Add new: `IFX_CFG_CLOCK_PPUPLL_FREQUENCY` (450000000) and `IFX_CFG_CLOCK_PERPLL3_FREQUENCY` (200000000)

2. **Board include** -- Add `#define IFX_BOARD KIT_TC4D7_LITE` and `#include "board.h"` if the template uses the board abstraction.

3. **Device define** -- Change `DEVICE_TC38X` (or `DEVICE_TC37X`) to `DEVICE_TC4DX`.

4. **CPU priority macros** -- TC4xx needs 6 entries (`IFX_CFG_CPU0_PRIO` through `IFX_CFG_CPU5_PRIO`), TC3xx only has 3-4.

5. **STM resolution** -- If present, set `IFX_STM_RESOULTION` to `IFX_CFG_CLOCK_SYSPLL_FREQUENCY`.

6. **Pin package defines** -- Remove TC3xx pin package defines (e.g., `IFX_PIN_PACKAGE_516`). TC4xx handles this differently through the board abstraction.

7. **OneEye config** (if applicable) -- Set `IFX_CFG_OE_AL_UC_VARIANT` to `IFX_CFG_OE_AL_UC_VARIANT_AURIX_ILLD_TC4`.

### STEP 5: Migrate `Cpu0_Main.c`

Start from the TC4xx template's `Cpu0_Main.c` structure and port the application logic:

1. **Includes** -- Replace:
   - `#include "IfxScuWdt.h"` -> `#include "IfxWtu.h"`
   - Add `#include "Ifx_Cfg.h"` if not present

2. **Function signature** -- Change `int core0_main(void)` to `void core0_main(void)`. Remove any `return (1);` at the end.

3. **Watchdog calls** -- Replace:
   - `IfxScuWdt_disableCpuWatchdog(IfxScuWdt_getCpuWatchdogPassword())` -> `IfxWtu_disableCpuWatchdog(IfxWtu_getCpuWatchdogPassword())`
   - `IfxScuWdt_disableSafetyWatchdog(IfxScuWdt_getSafetyWatchdogPassword())` -> `IfxWtu_disableSystemWatchdog(IfxWtu_getSystemWatchdogPassword())`
   Note: "Safety" watchdog is renamed to "System" watchdog in TC4xx.

4. **STM tick function** -- Replace:
   - `IfxStm_getTicksFromMilliseconds(BSP_DEFAULT_TIMER, WAIT_TIME)` -> `IfxStm_getTicksFromMilliseconds(WAIT_TIME)`
   The `BSP_DEFAULT_TIMER` parameter is removed in iLLD v2.x.

5. **Sync event** -- Add `IFX_ALIGN(4)` before `IfxCpu_syncEvent g_cpuSyncEvent = 0;` if not present.

6. **Doxygen header** -- Update the `\name`, `\board`, `\keywords`, and `\description` fields to reflect TC4xx.

7. **Application-specific init/loop calls** -- Port these from the TC3xx source, but rename any peripheral function calls per Step 6.

### STEP 6: Migrate Peripheral-Specific Application Code

This is the most variable part. Apply these known peripheral API mappings:

#### GTM -> EGTM (Timer/PWM)
- All `IfxGtm_*` types/functions -> `IfxEgtm_*`
- `MODULE_GTM` -> `MODULE_EGTM`
- `IfxGtm_Pwm_Config` -> `IfxEgtm_Pwm_Config`
- `IfxGtm_Pwm_ChannelConfig` -> `IfxEgtm_Pwm_ChannelConfig`
- `IfxGtm_Pwm_DtmConfig` -> `IfxEgtm_Pwm_DtmConfig`
- `IfxGtm_Pwm_InterruptConfig` -> `IfxEgtm_Pwm_InterruptConfig`
- `IfxGtm_Pwm_OutputConfig` -> `IfxEgtm_Pwm_OutputConfig`
- `IfxGtm_Pwm_init()` -> `IfxEgtm_Pwm_init()`
- `IfxGtm_Pwm_initConfig()` -> `IfxEgtm_Pwm_initConfig()`
- `IfxGtm_Pwm_ToutMap` -> `IfxEgtm_Pwm_ToutMap`
- TC4xx adds `interruptConfig.vmId = IfxSrc_VmId_0;` (VM ID for interrupt routing)
- TC4xx uses `IfxEgtm_Pwm_updateChannelsDutyImmediate()` for duty updates
- The old `IfxGtm_Tom_PwmHl_*` API is replaced by the unified `IfxEgtm_Pwm_*` API
- File naming: `GTM_TOM_*.c/.h` -> `EGTM_ATOM_*.c/.h`
- Reference TC3xx: `code_examples/iLLD_TC387_ADS_GTM_TOM_3_Phase_Inverter_PWM_2/GTM_TOM_3_Phase_Inverter_PWM.c`
- Reference TC4xx: `code_examples/iLLD_TC4D7_LK_ADS_EGTM_ATOM_3_Phase_Inverter_PWM_1/EGTM_ATOM_3_Phase_Inverter_PWM.c`

#### EVADC -> ADC (Analog-to-Digital)
- TC3xx EVADC module is replaced by a different ADC module in TC4xx
- `initEVADC()` style functions -> `initTMADC()` style functions
- Reference TC4xx: `code_examples/iLLD_TC4D7_LK_ADS_ADC_Single_Channel/ADC_Single_Channel.c`

#### ScuWdt -> Wtu (Watchdog)
- Already covered in Step 5

#### SPI/QSPI
- Check if the TC4xx iLLD v2.x has API changes for QSPI. Compare with `code_examples/iLLD_TC387_ADS_QSPI_TLE9180D_2/` if a TC4xx QSPI example exists.

#### Port (GPIO)
- `IfxPort_*` API is largely similar but pin mappings differ between TC3xx and TC4xx boards. Check the TC4xx board's pin.h file.

#### For any peripheral NOT listed above:
- Search the TC4xx iLLD headers in `Libraries/iLLD/TC4xx/` for the equivalent module
- The general pattern is: prefix changes from `Ifx<Module>` to `Ifx<NewModule>`, and `MODULE_<NAME>` SFR references change accordingly
- If no TC4xx equivalent exists in the repo examples, flag it to the user

### STEP 7: Update `.cproject` Include Paths

If you're modifying the `.cproject` rather than using the template's version:
1. Replace all `TC37A` or `TC38A` path references with `TC4DA` (or appropriate TC4xx variant)
2. Replace `TC3xx` with `TC4xx` in library paths
3. Change CPU derivative from e.g., `tc37x` or `tc38x` to `tc4dax`
4. Add include paths for any new TC4xx-specific directories (e.g., `Boards/`, `Boards/KIT_TC4D7_LITE/`)

### STEP 8: Update Documentation

1. Update `README.md` to reference TC4xx device, board, and any changed peripheral names.
2. Update the Doxygen header in `Cpu0_Main.c` with correct `\name`, `\board`, `\keywords`.

### STEP 9: Verify Completeness

Run this checklist:
- [ ] Project directory named `iLLD_TC4D7_LK_ADS_<NAME>/`
- [ ] `.ads/install-libraries.json` has TC4xx device maps and iLLD v2.x zip references
- [ ] `Configurations/Ifx_Cfg.h` uses `IFX_CFG_CLOCK_*` macros (not `IFX_CFG_SCU_*`)
- [ ] `Boards/board.h` exists with `IFX_DEVICE_FAMILY_TC4`
- [ ] Linker script has `__TRICORE_DERIVATE_MEMORY_MAP__ = 0x4D0` and 6 cores
- [ ] `Cpu0_Main.c` uses `IfxWtu_*` (not `IfxScuWdt_*`), `void core0_main` (not `int`), no `return (1)`
- [ ] `IfxStm_getTicksFromMilliseconds` has single argument (no `BSP_DEFAULT_TIMER`)
- [ ] `Cpu1_Main.c` through `Cpu5_Main.c` all exist
- [ ] `.cproject` references `tc4dax` derivative and `TC4xx`/`TC4DA` include paths
- [ ] No remaining references to `IfxScuWdt`, `MODULE_GTM` (should be `MODULE_EGTM`), `TC37`, `TC38`, `TC39`, or `iLLD_1_` in any source file
- [ ] Application-specific peripheral calls use TC4xx iLLD v2.x API names
- [ ] `g_cpuSyncEvent` has `IFX_ALIGN(4)` attribute

## Specifications

- The migrated project must compile against iLLD v2.x headers (no iLLD v1.x references remain)
- All infrastructure files (linker script, `.cproject`, `.ads/` configs, board files) must be TC4xx-native
- Peripheral API calls must use TC4xx iLLD v2.x naming conventions
- The project must target 6 cores (CPU0-CPU5) with proper CSA/stack allocations
- Validation: Run the Step 9 checklist. Verify no remaining TC3xx references via search for `IfxScuWdt`, `MODULE_GTM`, `TC37`, `TC38`, `TC39`, `iLLD_1_`

## Advice and Pointers

- **Pin mappings are board-specific** -- TC3xx pin assignments (e.g., P13.0 for LED) will likely differ on TC4xx boards. Check the TC4xx board schematic or pin.h.
- **The `Bsp.h` include** -- TC4xx examples still use `Bsp.h` for `waitTime()`, but the underlying implementation differs.
- **Interrupt routing** -- TC4xx introduces VM IDs (`IfxSrc_VmId_0`) for interrupt service provider routing. Any ISR configuration needs this added.
- **Memory sizes** -- TC4xx has different flash/RAM sizes. The linker script from the TC4xx template handles this, but custom memory sections from TC3xx may need adjustment.
- **`static` vs stack variables** -- TC4xx examples tend to use stack-allocated config structs instead of `static` local variables. This is a style preference in iLLD v2.x, not a hard requirement.

## Forbidden Actions

- **Do not mix iLLD v1.x and v2.x headers** -- they are incompatible.
- **Do not modify a TC3xx project in-place** -- always start from a TC4xx template.
- **Do not reuse TC3xx linker scripts, `.cproject`, or `.ads/` config files** -- the infrastructure differences are too large.
- **Do not keep TC3xx watchdog API calls** (`IfxScuWdt_*`) -- they must all be replaced with `IfxWtu_*`.
- **Do not keep `int core0_main(void)` signature** -- TC4xx uses `void core0_main(void)`.

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
