# Junction - Remaining Work & Prioritization

**Last Updated:** 2026-02-10
**Status:** Core functionality complete, Matrix bridge operational, polish and testing remain

---

## Current State Summary

### ✅ Completed (High Confidence)

1. **Core SMS Functionality**
   - ✅ SMS send/receive working
   - ✅ Conversation list and threading
   - ✅ Contact resolution and display
   - ✅ Notifications with proper channels
   - ✅ Default SMS app role handling
   - ✅ Multi-SIM support (from AOSP)

2. **Architecture & Abstraction**
   - ✅ 5 core interfaces defined (`SmsTransport`, `MessageStore`, `NotificationFacade`, `ContactResolver`, `SmsReceiver`)
   - ✅ 5 adapter implementations in `sms-upstream/adapter/`
   - ✅ Dependency injection via `CoreSmsRegistry`
   - ✅ App module fully decoupled from AOSP internals
   - ✅ 132 unit tests (contract + adapter tests)
   - ✅ Zero direct `com.android.messaging.*` imports in app module

3. **Matrix Bridge Integration**
   - ✅ Trixnity SDK 5.1.0 fully integrated (migrated from 4.22.7)
   - ✅ Matrix client authentication (password login)
   - ✅ Matrix session persistence and restoration
   - ✅ Matrix sync loop via foreground service
   - ✅ Room mapping (phone number ↔ Matrix room ID)
   - ✅ Room creation with canonical aliases (`#sms_+12345:server`)
   - ✅ Bidirectional SMS ↔ Matrix text message bridging
   - ✅ Matrix control room creation for device status
   - ✅ Matrix configuration UI screen
   - ✅ Auto-start sync service on config screen open
   - ✅ Custom state events for device status (signal, data, battery placeholder)

4. **Build & Tooling**
   - ✅ Gradle 9.3.1, AGP 9.0.0, Kotlin 2.3.10
   - ✅ Multi-module architecture (5 modules)
   - ✅ Version catalog (`libs.versions.toml`)
   - ✅ 16 AOSP patches documented in `PATCHES.md`
   - ✅ Clean builds (no errors, deprecation warnings noted)
   - ✅ Debug APK installable and functional

5. **UI & UX Fixes**
   - ✅ Edge-to-edge compatibility (API 35+)
   - ✅ Dark mode support (DayNight theme)
   - ✅ Modern IME inset handling (keyboard no longer overlays input field)
   - ✅ ActionBar padding for edge-to-edge
   - ✅ Matrix config screen edge-to-edge fixed

6. **Documentation**
   - ✅ `CLAUDE.md` - Project instructions for AI assistance
   - ✅ `README.md` - Project overview and build instructions
   - ✅ `ARCHITECTURE.md` - Design principles and data flow
   - ✅ `PATCHES.md` - All 16 upstream modifications documented
   - ✅ `UPSTREAM_UPDATE_GUIDE.md` - Step-by-step update process
   - ✅ `TRIXNITY_5.0_MIGRATION.md` - Migration guide
   - ✅ `TRIXNITY_IMPLEMENTATION_COMPLETE.md` - API usage documentation

---

## Remaining Work - Prioritized

### 🔴 Priority 1: Critical Functionality Gaps

#### 1.1 MMS Functionality Verification
**Status:** 🟡 Partially tested - BLOCKED by cellular connectivity
**Effort:** 2-4 hours (manual testing) - 0.5 hours completed
**Risk:** High (MMS is core SMS app functionality)

**Tasks:**
- [x] Add missing storage permissions (READ_MEDIA_*, CAMERA)
- [x] Create comprehensive MMS testing guide
- [x] Test MMS send with image attachment (BLOCKED - no cellular)
- [ ] Test MMS send with video attachment
- [ ] Test MMS receive with media
- [ ] Test MMS group messaging
- [ ] Document any MMS limitations or bugs
- [ ] Fix MMS issues if found

**Progress:**
- ✅ Fixed critical bug: Missing storage permissions in manifest
- ✅ Created MMS_TESTING_GUIDE.md with 8 test cases
- 🟡 Initial test blocked: Device on Wi-Fi only, no cellular signal
- 📋 Root cause identified: MMS requires cellular data, device shows OUT_OF_SERVICE

**Rationale:** MMS is essential for a production SMS app. AOSP code is present but untested in Junction.

**Blockers:** Requires cellular network connection (currently Wi-Fi only)
**Testing:** Resume manual testing when cellular signal available

---

#### 1.2 Matrix Media Handling (Images, Videos, Audio)
**Status:** TODO (stubs present in `TrixnityMatrixBridge.kt:126-143`)
**Effort:** 8-16 hours
**Risk:** High (core Matrix bridge feature)

**Tasks:**
- [ ] Implement SMS MMS image → Matrix image upload
- [ ] Implement SMS MMS video → Matrix video upload
- [ ] Implement SMS MMS audio → Matrix audio upload
- [ ] Implement Matrix image → SMS MMS download and send
- [ ] Implement Matrix video → SMS MMS
- [ ] Implement Matrix audio → SMS MMS
- [ ] Handle file size limits (MMS 300KB-600KB carrier limits vs Matrix)
- [ ] Implement media compression/transcoding if needed
- [ ] Add media upload progress indicators

**Rationale:** Without media support, the Matrix bridge is text-only, severely limiting usefulness.

**API Reference:**
```kotlin
// Trixnity 5.x media upload
client.media.upload(mediaFile: ByteArray, contentType: String): Result<MxcUrl>
client.media.download(mxcUrl: MxcUrl): Result<ByteArray>

// Message sending with media
client.room.sendMessage(roomId) {
    image(mxcUrl, body = "filename.jpg", info = ImageInfo(...))
    video(mxcUrl, body = "video.mp4", info = VideoInfo(...))
    audio(mxcUrl, body = "audio.m4a", info = AudioInfo(...))
}
```

**Blockers:** None (Trixnity APIs available)
**Testing:**
- Send MMS with image from phone → check Matrix shows image
- Send image in Matrix → check MMS arrives with attachment

---

### 🟡 Priority 2: Stability & Compatibility

#### 2.1 GrapheneOS Full Compatibility Testing
**Status:** Partial (app runs but not fully tested)
**Effort:** 4-8 hours
**Risk:** Medium (target platform compatibility)

**Tasks:**
- [ ] Test on real GrapheneOS device (Pixel 9a confirmed working)
- [ ] Verify all SMS permissions work without system signature
- [ ] Test default SMS app selection flow
- [ ] Test MMS over data vs Wi-Fi (APN handling)
- [ ] Verify notifications work with GrapheneOS notification channels
- [ ] Test with hardened_malloc and other GrapheneOS hardening
- [ ] Document any GrapheneOS-specific quirks or workarounds
- [ ] Test with multiple user profiles (GrapheneOS feature)

**Rationale:** GrapheneOS is the primary target platform. Full testing ensures compatibility.

**Blockers:** Need GrapheneOS device access
**Testing:** Manual testing on GrapheneOS Pixel device

---

#### 2.2 Stub Library Replacements
**Status:** 3 stub libraries remain (rastermill, vcard, photo partially stubbed)
**Effort:** 8-12 hours
**Risk:** Low (stubs functional but incomplete)

**Tasks:**

1. **Replace `android.support.rastermill` (animated GIF handling)**
   - [ ] Add Glide dependency with GIF support
   - [ ] Replace FrameSequenceDrawable usage in message attachments
   - [ ] Delete stub files: `FrameSequence.java`, `FrameSequenceDrawable.java`
   - [ ] Test animated GIF display in conversations

2. **Replace `com.android.vcard` (vCard parsing)**
   - [ ] Add ez-vcard library dependency
   - [ ] Replace VCardEntry, VCardParser usage
   - [ ] Update CustomVCardEntry to use ez-vcard types
   - [ ] Delete 13 vcard stub files
   - [ ] Test contact vCard import/export

3. **Verify `com.android.ex.photo` (already full implementation)**
   - [ ] Confirm all photo viewer features work
   - [ ] No action needed (already vendored full library)

**Rationale:** Stubs compile but provide no functionality. Real libraries enable features.

**Blockers:** None
**Testing:**
- Send animated GIF via MMS
- Import/export contact vCard
- View attached photos in conversations

---

#### 2.3 Matrix Encryption (E2EE) Support
**Status:** Not implemented (Trixnity supports it)
**Effort:** 16-24 hours
**Risk:** Medium (security feature)

**Tasks:**
- [ ] Enable Trixnity crypto module (`cryptoDriverModule`)
- [ ] Implement key storage (SQLCipher or encrypted SharedPreferences)
- [ ] Handle device verification flow
- [ ] Add UI for key backup/restore
- [ ] Test encrypted room bridging
- [ ] Document encryption limitations (SMS is plaintext, Matrix is encrypted)

**Rationale:** Many Matrix users expect E2EE. Without it, the bridge is insecure on Matrix side.

**Blockers:** Requires decision on key storage strategy
**Testing:**
- Create encrypted Matrix room
- Bridge SMS to encrypted room
- Verify messages decrypt correctly

---

### 🟢 Priority 3: Polish & UX Improvements

#### 3.1 Error Handling & User Feedback
**Status:** Basic error handling present, needs improvement
**Effort:** 6-10 hours
**Risk:** Low

**Tasks:**
- [ ] Add Toast/Snackbar messages for common errors
- [ ] Improve Matrix connection error messages
- [ ] Add retry logic for failed Matrix messages
- [ ] Show progress indicators for Matrix operations
- [ ] Handle network offline gracefully (queue messages)
- [ ] Add error reporting mechanism (logs, crash handler)

---

#### 3.2 Matrix Presence Tracking (Real Implementation)
**Status:** Stub present in `MatrixBridgeInitializer.kt:155`
**Effort:** 4-6 hours
**Risk:** Low

**Tasks:**
- [ ] Implement real signal strength monitoring (TelephonyManager)
- [ ] Implement real data connectivity monitoring (ConnectivityManager)
- [ ] Implement real battery level monitoring (BatteryManager)
- [ ] Send device status updates to Matrix control room every 5-10 minutes
- [ ] Add UI to show device status in Matrix config screen

**Current Stub:**
```kotlin
// TODO: Implement real presence tracking.
val status = DeviceStatus(
    signal = "Unknown",
    data = "Unknown",
    battery = 100
)
```

**Rationale:** Provides visibility into device health for remote monitoring.

---

#### 3.3 UI Refinements
**Status:** Functional but minimal
**Effort:** 8-16 hours
**Risk:** Low

**Tasks:**
- [ ] Add loading states to Matrix config screen
- [ ] Improve Matrix login error messages
- [ ] Add "Connected to Matrix" indicator in conversation list
- [ ] Add settings screen for Matrix configuration
- [ ] Add Matrix room list view (see all bridged conversations)
- [ ] Add ability to manually create/delete room mappings
- [ ] Polish notification icons and text
- [ ] Add app icon (currently using default)

---

### 🔵 Priority 4: Testing & Quality Assurance

#### 4.1 Integration Tests
**Status:** None exist
**Effort:** 12-20 hours
**Risk:** Low (app works, tests validate)

**Tasks:**
- [ ] Add Espresso UI tests for core flows:
  - [ ] Open app → select default SMS app
  - [ ] Compose new message → send SMS
  - [ ] Receive SMS → notification appears
  - [ ] Open conversation → view messages
  - [ ] Matrix login flow
  - [ ] Matrix sync service start/stop
- [ ] Add instrumented tests for adapters (requires device)
- [ ] CI setup for automated testing (if desired)

**Rationale:** Integration tests catch regressions during AOSP updates.

---

#### 4.2 Manual Testing Checklist
**Status:** Partial (SMS tested, MMS/Matrix not fully)
**Effort:** 4-6 hours
**Risk:** Low

**Tasks:**
- [ ] Create manual test plan document
- [ ] Test SMS send to multiple carriers (Verizon, AT&T, T-Mobile)
- [ ] Test MMS with various attachment types
- [ ] Test Matrix bridging with public homeserver (matrix.org)
- [ ] Test Matrix bridging with self-hosted homeserver (Synapse)
- [ ] Test edge cases (airplane mode, no data, low battery)
- [ ] Test on multiple Android versions (API 29-36)
- [ ] Performance profiling (memory leaks, ANRs)

---

#### 4.3 Expand Unit Test Coverage
**Status:** 132 tests exist (good foundation)
**Effort:** 6-10 hours
**Risk:** Low

**Tasks:**
- [ ] Add tests for Matrix bridge logic (room mapping, message conversion)
- [ ] Add tests for Matrix sync service lifecycle
- [ ] Increase coverage to 80%+ (currently unknown)
- [ ] Add Robolectric tests for app module (currently none)

---

### ⚪ Priority 5: Future Enhancements (Post-MVP)

#### 5.1 Matrix Room Database Migration
**Status:** Currently using SharedPreferences (acceptable for < 100 contacts)
**Effort:** 8-12 hours
**Risk:** Low

**Tasks:**
- [ ] Migrate room mapping from SharedPreferences to Room database
- [ ] Add migration logic to preserve existing mappings
- [ ] Add Room database for Matrix message history (optional)
- [ ] Improve performance for users with many contacts

**Note:** KSP 2.3.5 is now compatible (migration guide exists in `ROOM_MIGRATION_GUIDE.md`)

---

#### 5.2 Advanced Matrix Features
**Status:** Not implemented
**Effort:** Varies
**Risk:** Low

**Tasks:**
- [ ] Matrix read receipts → SMS delivery reports
- [ ] Matrix typing indicators → SMS composing indicators
- [ ] Matrix reactions → SMS reactions (RCS)
- [ ] Matrix threads → SMS threading
- [ ] Matrix space support (organize bridged rooms)
- [ ] Matrix bridge bot commands (`!bridge help`, `!bridge map`, etc.)

---

#### 5.3 Multi-Device Matrix Sync
**Status:** Single device only
**Effort:** 12-16 hours
**Risk:** Medium

**Tasks:**
- [ ] Support multiple Android devices bridging same phone number
- [ ] Implement Matrix application service (AS) pattern instead of user bot
- [ ] Deploy bridge server (optional, for shared homeserver)

---

#### 5.4 Performance Optimizations
**Status:** Not profiled
**Effort:** 8-12 hours
**Risk:** Low

**Tasks:**
- [ ] Profile memory usage (Android Profiler)
- [ ] Optimize database queries (currently using AOSP queries)
- [ ] Reduce APK size (currently 49MB debug, ProGuard not applied)
- [ ] Optimize Matrix sync loop (reduce battery drain)
- [ ] Add WorkManager for background tasks instead of foreground service

---

## Risk Assessment

### High-Risk Items (Could Block Production Use)
1. **MMS verification** - If broken, app is unusable for many users
2. **Matrix media handling** - Without it, bridge is text-only
3. **GrapheneOS compatibility** - Primary target platform

### Medium-Risk Items (Reduce Quality/Security)
1. **Matrix encryption** - Security issue if not implemented
2. **Error handling** - Poor UX without good error messages

### Low-Risk Items (Nice-to-Have)
1. **Stub library replacements** - Current stubs work, just incomplete
2. **Integration tests** - App works, tests just validate
3. **UI polish** - Functional but not pretty

---

## Recommended Execution Plan

### Phase 1: Critical Path (1-2 weeks)
1. **MMS verification** (Priority 1.1) - 4 hours
2. **Matrix media handling** (Priority 1.2) - 16 hours
3. **GrapheneOS testing** (Priority 2.1) - 8 hours

**Deliverable:** Production-ready SMS/MMS ↔ Matrix bridge with media support

---

### Phase 2: Stability (1 week)
1. **Error handling** (Priority 3.1) - 10 hours
2. **Stub library replacements** (Priority 2.2) - 12 hours
3. **Integration tests** (Priority 4.1) - 12 hours

**Deliverable:** Stable, well-tested app with all features working

---

### Phase 3: Security (1 week)
1. **Matrix encryption** (Priority 2.3) - 24 hours
2. **Manual testing** (Priority 4.2) - 6 hours
3. **Documentation updates** (Priority 3.3) - 4 hours

**Deliverable:** Secure, documented, fully-tested production app

---

### Phase 4: Polish (Ongoing)
1. **UI refinements** (Priority 3.3) - 16 hours
2. **Presence tracking** (Priority 3.2) - 6 hours
3. **Advanced features** (Priority 5.x) - As desired

**Deliverable:** Polished, feature-rich application

---

## Success Criteria

**Minimum Viable Product (MVP):**
- ✅ SMS send/receive works reliably
- ⏳ MMS send/receive works with attachments
- ⏳ Matrix ↔ SMS text bridging works
- ⏳ Matrix ↔ SMS media bridging works
- ⏳ Works on GrapheneOS without root
- ⏳ No crashes or data loss

**Production Ready:**
- All MVP criteria met
- ⏳ Integration tests pass
- ⏳ Manual testing completed across carriers
- ⏳ Error handling provides clear user feedback
- ⏳ Documentation complete and accurate
- ⏳ Matrix encryption enabled (or documented as limitation)

**Polished Release:**
- All Production Ready criteria met
- ⏳ UI refined with proper icons and branding
- ⏳ Advanced Matrix features implemented
- ⏳ Performance optimized (< 100MB memory, < 5% battery drain)
- ⏳ User guide and FAQ documentation

---

## Known Issues & Limitations

### Current Limitations
1. **No Matrix encryption** - Messages bridged to unencrypted rooms (documented in CLAUDE.md)
2. **SharedPreferences for room mapping** - Acceptable for < 100 contacts, but Room DB preferred (migration guide exists)
3. **No MMS verification** - Unknown if MMS works end-to-end
4. **Stub libraries** - Animated GIFs and vCard import/export non-functional
5. **Single device only** - Cannot bridge same phone number from multiple Android devices

### Non-Issues (Resolved)
- ✅ Keyboard overlay (fixed with PATCH-016)
- ✅ Edge-to-edge layout issues (fixed)
- ✅ Matrix sync service crashes (fixed)
- ✅ Trixnity 5.x migration (completed)

---

## Conclusion

**Current Status:** ~80% complete for MVP, ~60% complete for Production Ready

**Recommended Next Steps:**
1. Verify MMS functionality (4 hours) - **CRITICAL**
2. Implement Matrix media handling (16 hours) - **CRITICAL**
3. Test on GrapheneOS (8 hours) - **HIGH PRIORITY**
4. Add error handling and user feedback (10 hours) - **HIGH PRIORITY**

After these 4 items (38 hours / ~1 week of focused work), the app will be production-ready for basic SMS ↔ Matrix bridging with media support on GrapheneOS.

All remaining items are polish, optimization, and advanced features that can be added iteratively based on user feedback.
