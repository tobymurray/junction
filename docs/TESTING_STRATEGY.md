# Testing Strategy

This document defines the testing approach for the AospMessaging project, following a **hybrid strategy** that balances speed, reliability, and coverage.

## Test Types

### 1. JVM Unit Tests (Fast, Run in CI)
**Location:** `core-sms/src/test/`, `sms-upstream/src/test/`, `app/src/test/`

**Framework:** JUnit 4 + MockK + Coroutines Test + Turbine

**Purpose:** Test pure logic without Android dependencies

**What to test here:**
- Interface contracts (already implemented in core-sms)
- Business logic
- Data transformations
- Error handling
- Pure Kotlin/Java code

**Examples:**
- `CoreSmsRegistryTest` - DI registry logic
- `SmsTransportContractTest` - Interface contract validation
- Data class validation

---

### 2. Robolectric Tests (Medium Speed, Run in CI)
**Location:** `sms-upstream/src/test/` (alongside JVM tests)

**Framework:** Robolectric + JUnit 4 + MockK

**Purpose:** Test Android framework interactions on the JVM without a device

**What to test here:**
- **Adapter implementations** (SmsTransportImpl, MessageStoreImpl, etc.)
- Data transformation between core-sms and AOSP types
- Android API calls (Context, ContentResolver, SmsManager)
- ContentProvider queries
- Delegation to AOSP code
- Error handling with Android exceptions

**Examples:**
- `SmsTransportImplTest` - Verify adapter delegates to SmsSender correctly
- `MessageStoreImplTest` - Verify ContentProvider queries work
- `ContactResolverImplTest` - Verify ContactsContract queries work

**What NOT to test here:**
- Actual SMS/MMS radio transmission (use instrumented tests)
- Actual permission enforcement (use instrumented tests)
- Complex UI interactions (use instrumented tests)

---

### 3. Instrumented Tests (Slow, Device Required, Run Manually)
**Location:** `sms-upstream/src/androidTest/`, `app/src/androidTest/`

**Framework:** AndroidX Test + Espresso (for UI)

**Purpose:** Test critical paths that require real Android framework

**What to test here (MINIMAL SET):**
- **Happy path SMS flow:** Send SMS → Receive SMS → Display in conversation
- **Permission handling:** Request SMS permissions, verify grant/deny
- **MMS send/receive:** If critical (debate: may be manual testing only)

**What NOT to test here:**
- Anything that can be tested with Robolectric
- Edge cases (use Robolectric for those)
- Business logic (use JVM unit tests)

**Examples:**
- `SmsSendReceiveIntegrationTest` - End-to-end SMS flow on real device
- `PermissionsIntegrationTest` - Verify permission request/grant flow

---

## Testing Boundaries by Component

### Core-sms Module
| Component | Test Type | Rationale |
|-----------|-----------|-----------|
| Interface definitions | JVM Unit Tests | Pure Kotlin, no Android deps |
| Data classes | JVM Unit Tests | Pure Kotlin data classes |
| CoreSmsRegistry | JVM Unit Tests | DI logic, no Android deps |

**Current:** 6 test classes, 110 tests (all JVM unit tests) ✅

---

### Sms-upstream Module
| Component | Test Type | Rationale |
|-----------|-----------|-----------|
| Adapter instantiation | **Robolectric** | Verify adapters can be created |
| Interface compliance | **Robolectric** | Verify adapters implement contracts |
| Data transformation | **Instrumented** (deferred) | Requires AOSP Factory initialization |
| Error handling | **Instrumented** (deferred) | Requires AOSP Factory initialization |
| AOSP code (vendored) | **Not tested** | Assumed correct from upstream |
| Critical SMS/MMS paths | **Instrumented** (future) | Requires real radio/framework |

**Current:** 5 adapter test classes (lightweight Robolectric) + contract tests

**Pragmatic Tradeoff:**
Due to AOSP's heavy use of Factory singleton pattern, deep adapter testing would
require extensive mocking or refactoring that violates architectural constraints.
Instead, we rely on:
1. Contract tests (110 tests) - validate expected interface behavior
2. Lightweight adapter tests (4 tests) - verify creation & compliance
3. Manual testing - developer validation on real devices
4. Future instrumented tests - critical paths only

---

### App Module
| Component | Test Type | Rationale |
|-----------|-----------|-----------|
| MainActivity | **Robolectric** (future) | Intent handling, no device needed |
| Receivers | **Robolectric** (future) | Intent extraction, delegation to core-sms |
| Services | **Robolectric** (future) | Headless sending logic |
| UI flows | **Instrumented** (future) | User interactions, optional for now |

**Current:** No tests yet (deferred - app is thin trampoline)

---

## Test Execution Strategy

### Local Development
```bash
# Fast: Run all JVM tests (contract + Robolectric)
./gradlew test

# Slow: Run instrumented tests (requires device)
./gradlew connectedAndroidTest
```

### Continuous Integration
```bash
# CI runs JVM tests automatically on every commit
./gradlew test

# Instrumented tests run:
# - Manually before releases
# - On dedicated test devices
# - Not on every commit (too slow)
```

---

## Robolectric vs Instrumented Decision Tree

```
Does the test require...
├─ Real SMS radio transmission? → Instrumented
├─ Real permission enforcement? → Instrumented
├─ Real system provider writes? → Instrumented
├─ Actual device hardware? → Instrumented
└─ None of the above? → Robolectric
```

**Default to Robolectric unless you have a specific reason to use instrumented tests.**

---

## Current Test Coverage

| Module | JVM Unit | Robolectric | Instrumented | Total |
|--------|----------|-------------|--------------|-------|
| core-sms | 110 | 0 | 0 | 110 |
| sms-upstream | 0 | 4 | 0 | 4 |
| app | 0 | 0 | 0 | 0 |
| **Total** | **110** | **4** | **0** | **114** |

**Implementation Status:**
- ✅ Contract tests (110) - All 5 interfaces fully specified
- ✅ Adapter tests (4) - Lightweight Robolectric tests for SmsTransportImpl
- ⏳ Additional adapters (TODO) - MessageStore, ContactResolver, NotificationFacade, SmsReceiverDispatcher
- ⏳ Instrumented tests (TODO) - Minimal critical path tests
- ⏳ App tests (DEFERRED) - Low priority, thin trampoline

---

## Testability Guidelines

### Writing Testable Adapters

**✅ Good:**
```kotlin
class SmsTransportImpl(
    private val context: Context,
    private val smsSender: SmsSender = Factory.get().getSmsSender()
) : SmsTransport {
    // Core logic testable with mock smsSender
}
```

**❌ Bad:**
```kotlin
class SmsTransportImpl : SmsTransport {
    override fun sendSms(...) {
        // Directly creates dependencies - hard to test
        val sender = Factory.get().getSmsSender()
        sender.send(...)
    }
}
```

**Principle:** Inject dependencies to enable mocking in tests.

---

## Test Quality Standards

1. **Fast:** JVM tests should run in < 5 seconds total
2. **Deterministic:** No flaky tests, no sleep/wait calls
3. **Isolated:** Each test is independent
4. **Clear:** Test name describes what is tested
5. **Minimal:** Only test what needs testing (no redundancy)

---

## Anti-Patterns to Avoid

❌ **Over-mocking Android internals**
- Don't mock every ContentResolver call
- Use Robolectric to provide real-ish Android components

❌ **Testing implementation details**
- Test behavior, not internal state
- Focus on contracts, not private methods

❌ **Too many instrumented tests**
- Keep instrumented tests < 10% of total tests
- Prefer Robolectric for most Android testing

❌ **Slow tests**
- No Thread.sleep() in tests
- Use coroutine test utilities
- Mock network calls

---

## Pragmatic Approach: Why Lightweight Adapter Tests?

### Architectural Constraint
AOSP Messaging uses the **Factory singleton pattern** extensively:
```java
// AOSP pattern - hard to test
SmsSender.sendMessage(...) // Static/singleton access
PhoneUtils.getDefault().getSubscriptionInfo() // Singleton
Factory.get().getDataModel() // Global factory
```

### Testing Challenges
1. **Deep dependency chains**: Adapters → Factory → DataModel → DatabaseHelper → Resources
2. **Singleton state**: Factory initialization requires full app context
3. **AOSP coupling**: Cannot easily inject mocks without refactoring AOSP code

### Considered Approaches
| Approach | Pros | Cons | Decision |
|----------|------|------|----------|
| Deep mocking | Full coverage | 100s of mocks, brittle | ❌ Rejected |
| Refactor adapters | Testable | Violates architecture | ❌ Rejected |
| Full AOSP init | Real testing | Slow, complex setup | ❌ Rejected |
| **Lightweight + Instrumented** | **Pragmatic, fast** | **Less unit test coverage** | ✅ **Adopted** |

### Adopted Strategy
**Layer testing across three levels:**

1. **Contract tests (110 tests)** - Define expected behavior
   - What: Interface specifications
   - Where: `core-sms/src/test/`
   - Coverage: Complete interface contracts

2. **Lightweight adapter tests (4+ tests)** - Verify basics
   - What: Instantiation, interface compliance, type safety
   - Where: `sms-upstream/src/test/`
   - Coverage: Creation & API surface

3. **Instrumented tests (future)** - Critical paths only
   - What: Real SMS send/receive, permissions
   - Where: `sms-upstream/src/androidTest/`
   - Coverage: End-to-end happy paths

**Result:** Good coverage without over-engineering, optimized for solo development.

---

## Version History

| Date | Change |
|------|--------|
| 2026-02-08 | Initial testing strategy defined (hybrid approach) |
| 2026-02-08 | Adopted pragmatic lightweight approach for adapters |
