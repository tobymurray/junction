# CLAUDE.md

This file provides authoritative engineering guidance for Claude Code when modifying this repository.  
It defines architectural boundaries, system invariants, reliability guarantees, and non-negotiable constraints.

This document is a contract. Architectural violations are defects.

---

# Project Overview

Junction is a fork of AOSP Messaging (Android’s default SMS app) converted into a standalone Gradle-built application with strict architectural isolation and a bidirectional Matrix bridge (SMS ↔ Matrix).

## Core Objectives

- Build using standard Android Studio + Gradle tooling.
- Operate as the default SMS app on stock Android and GrapheneOS.
- Use only public Android SDK APIs (no hidden APIs, no system signatures).
- Enable mechanical upstream AOSP replacement.
- Provide production-grade, idempotent SMS ↔ Matrix synchronization.

---

# Architectural Philosophy

The system is built around **replaceable upstream code**, **interface boundaries**, and **idempotent distributed synchronization**.

Upstream AOSP must remain replaceable.
Matrix must never become the source of truth for SMS.
All cross-layer behavior must be explicit and testable.

---

# Module Architecture
```
app/ ← Application layer
├─ depends on ─→ core-sms/
├─ depends on ─→ core-matrix/
├─ runtime ─→ sms-upstream/
└─ runtime ─→ matrix-impl/

sms-upstream/ ← Vendored AOSP + adapters
└─ implements ─→ core-sms/

matrix-impl/ ← Trixnity implementation
└─ implements ─→ core-matrix/
```


---

# Strict Layering Rules

The app module must NEVER import:

- `com.android.messaging.*`
- Any implementation class from `matrix-impl/`

All interaction must go through `core-sms/` and `core-matrix/`.

If replacing `sms-upstream/src/` requires app changes, abstraction boundaries were violated.

---

# System Invariants (Must Never Be Violated)

## 1. SMS Database Is Source of Truth

- AOSP (Bugle DB) is authoritative for SMS/MMS.
- Matrix events are projections.
- Matrix state must never override carrier truth.

## 2. Idempotent Bridging

Both directions (SMS → Matrix and Matrix → SMS) must be:

- Crash safe
- Retry safe
- Duplicate resistant
- Order preserving

If process death occurs at any point, system must resume safely.

## 3. No Cross-Layer State Mutation

- `matrix-impl/` must not mutate AOSP DB directly.
- All SMS writes go through `SmsTransport` and `MessageStore`.

## 4. Upstream Replaceability

Replacing `sms-upstream/src/` wholesale must:

- Not require changes to `app/`
- Not require changes to `matrix-impl/`
- Only require adapter boundary fixes

## 5. Deterministic Routing

Every inbound/outbound message must have a deterministic mapping to:

- Conversation/thread
- Matrix room (if bridging enabled)

No routing decisions may depend on in-memory-only state.

---

# Bridging State Model

Messages maintain two independent state dimensions.

## Carrier State
- PENDING_CARRIER
- SENT_CARRIER
- DELIVERED_CARRIER
- FAILED_CARRIER

## Matrix Bridge State
- BRIDGE_PENDING
- BRIDGED
- BRIDGE_FAILED

Rules:

- States persist across restarts.
- States never regress.
- Matrix bridge never sends without stable local ID.
- Each message ID produces at most one Matrix event.

If redesigning state improves correctness, redesign it.

Do not preserve flawed state models.

---

# Dependency Injection Strategy

Registries:

- `CoreSmsRegistry`
- `MatrixRegistry`

Rules:

- Initialized once
- Immutable after initialization
- Thread-safe

Registries exist to avoid invasive upstream changes.

---

# Concurrency & Background Rules

System must tolerate:

- Process death
- WorkManager retries
- Device reboot
- Network interruption
- Sync replay

Rules:

1. No network on main thread.
2. All bridge operations background-safe.
3. No in-memory-only routing state.
4. All DB writes atomic.
5. All bridge logic idempotent.

If adding resilience requires schema redesign, perform the redesign.

---

# What Goes Where

## app/

- UI
- Receivers
- Services
- Role management
- Settings
- Bridge orchestration

Must NOT:
- Access AOSP DB directly.
- Call SmsManager directly.
- Call matrix-impl classes directly.

## core-sms/

Interfaces only.

## sms-upstream/

Vendored AOSP + adapters.
No app logic.
No Matrix logic.

## core-matrix/

Interfaces only.

## matrix-impl/

Trixnity 5.1.0 integration.

Must not access AOSP DB directly.

---

# Room Mapping Strategy

Identifier normalization:

- E.164
- short:<digits>
- unknown:<sender>

If current storage (SharedPreferences) is insufficient:

Replace it with Room database.
Do not preserve legacy mapping format.

Schema redesign is acceptable.

---

# Failure Scenario Requirements

All features must define behavior for:

- App killed during SMS send
- App killed during Matrix send
- Device reboot
- Network unavailable
- Duplicate Matrix event replay
- SIM swap
- SMS role revoked

No feature is complete without failure analysis.

---

# Updating Upstream AOSP

When updating:

1. Replace sms-upstream/src/ wholesale.
2. Reapply documented patches.
3. Repair adapter boundary.
4. Do not modify app layer to compensate.

If upstream update exposes architectural weakness, fix architecture — not compatibility.

---

# Testing Requirements

Every new feature must include:

- Idempotency test
- Restart recovery test
- Duplicate event simulation
- Failure scenario test

---

# Forbidden Patterns

❌ Direct upstream imports in app  
❌ App logic inside sms-upstream  
❌ Direct SmsManager usage  
❌ Direct AOSP DB access  
❌ Matrix calls from UI  
❌ Compatibility hacks to preserve old test data  

---

# Definition of Production Ready

A change is production ready only if:

- No duplicate bridging possible
- No message loss possible under crash
- No cross-layer leakage introduced
- No architecture weakened for compatibility
- Tests cover idempotency and restart safety
- Failure modes explicitly handled

---

# Guiding Principle

Upstream must remain replaceable.  
Bridging must remain idempotent.  
State must remain deterministic.  
Architecture must not be compromised for backward compatibility.