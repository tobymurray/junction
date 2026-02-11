# MMS Testing Guide for Junction

**Date:** 2026-02-10
**Purpose:** Verify MMS send/receive functionality with various media types
**Priority:** CRITICAL (blocks Matrix media handling implementation)

---

## Prerequisites

- Junction installed as default SMS app
- Active cellular connection with MMS-capable plan
- Test phone number (friend/second device) for send/receive tests
- Wi-Fi enabled (some carriers require data for MMS)
- APN settings configured (should be automatic)

---

## Test Environment Setup

### Check Current MMS Configuration

1. **Verify APN Settings:**
   ```
   Settings → Network & Internet → Mobile Network → Advanced → Access Point Names
   ```
   - Ensure MMS APN is present
   - Note: GrapheneOS uses carrier-provided APN by default

2. **Verify Data Connection:**
   ```
   Settings → Network & Internet → Mobile Network
   ```
   - Mobile data must be enabled (even on Wi-Fi, MMS uses cellular data)

3. **Check Junction Permissions:**
   ```
   Settings → Apps → Junction → Permissions
   ```
   - SMS: Allowed
   - Phone: Allowed
   - Contacts: Allowed
   - Storage: Allowed (needed for attachments)

---

## Test Cases

### Test 1: MMS Send - Single Image

**Objective:** Verify Junction can send MMS with image attachment

**Steps:**
1. Open Junction app
2. Open existing conversation or start new one
3. Tap attachment button (paperclip icon)
4. Select "Gallery" or "Camera"
5. Choose a small image (< 500KB recommended)
6. Add optional message text: "Test MMS image 1"
7. Tap Send button
8. Observe sending progress

**Expected Result:**
- ✅ Message shows "Sending..." status
- ✅ Message transitions to "Sent" or "Delivered"
- ✅ Image thumbnail displays in conversation
- ✅ Recipient receives MMS with image

**Actual Result:**
- [ ] Pass / [ ] Fail
- Notes: _______________________________________

**If Failed, Check:**
- Logcat for errors: `adb logcat | grep -i mms`
- Network connectivity
- APN settings (MMS APN must exist)

---

### Test 2: MMS Send - Large Image (Compression)

**Objective:** Verify MMS image compression for carrier limits

**Steps:**
1. Open Junction
2. Open conversation
3. Attach large image (> 1MB)
4. Add text: "Test MMS large image"
5. Send

**Expected Result:**
- ✅ Junction compresses image to carrier limit (typically 300-600KB)
- ✅ Message sends successfully
- ✅ Recipient receives compressed but viewable image

**Actual Result:**
- [ ] Pass / [ ] Fail
- Notes: _______________________________________

---

### Test 3: MMS Send - Video

**Objective:** Verify video MMS send

**Steps:**
1. Open Junction
2. Open conversation
3. Tap attachment → "Gallery"
4. Select short video (< 30 seconds recommended)
5. Add text: "Test MMS video"
6. Send

**Expected Result:**
- ✅ Video thumbnail displays
- ✅ Junction may compress video to carrier limit
- ✅ Message sends
- ✅ Recipient receives playable video

**Actual Result:**
- [ ] Pass / [ ] Fail
- Notes: _______________________________________

---

### Test 4: MMS Send - Audio

**Objective:** Verify audio MMS send

**Steps:**
1. Open Junction
2. Open conversation
3. Tap attachment → Record audio or select audio file
4. Send

**Expected Result:**
- ✅ Audio attachment displays
- ✅ Message sends
- ✅ Recipient receives playable audio

**Actual Result:**
- [ ] Pass / [ ] Fail
- Notes: _______________________________________

---

### Test 5: MMS Receive - Image

**Objective:** Verify Junction receives incoming MMS with image

**Steps:**
1. Have someone send you MMS with image from another device
2. Wait for notification
3. Open Junction to view message

**Expected Result:**
- ✅ Notification displays with "Picture message" text
- ✅ Message appears in conversation with image thumbnail
- ✅ Tapping image opens full-screen viewer
- ✅ Image displays correctly (not corrupted)

**Actual Result:**
- [ ] Pass / [ ] Fail
- Notes: _______________________________________

---

### Test 6: MMS Receive - Video

**Objective:** Verify video MMS receive

**Steps:**
1. Have someone send you MMS with video
2. Wait for notification
3. Open Junction, tap video

**Expected Result:**
- ✅ Notification received
- ✅ Video thumbnail displays
- ✅ Tapping video plays it
- ✅ Video plays without errors

**Actual Result:**
- [ ] Pass / [ ] Fail
- Notes: _______________________________________

---

### Test 7: Group MMS

**Objective:** Verify MMS group messaging

**Steps:**
1. Open Junction
2. Create new message to multiple recipients (2-3 people)
3. Attach image
4. Add text: "Test group MMS"
5. Send

**Expected Result:**
- ✅ Message converts to MMS (multiple recipients require MMS)
- ✅ All recipients receive the message
- ✅ Replies appear in group thread

**Actual Result:**
- [ ] Pass / [ ] Fail
- Notes: _______________________________________

---

### Test 8: MMS over Wi-Fi Only

**Objective:** Verify MMS works with cellular data via Wi-Fi

**Steps:**
1. Disable mobile data: Settings → Network → Mobile data OFF
2. Connect to Wi-Fi
3. Send MMS with image
4. Observe behavior

**Expected Result:**
- ❓ May fail (many carriers require cellular data for MMS)
- ❓ Or may work if carrier supports Wi-Fi calling with MMS

**Actual Result:**
- [ ] Pass / [ ] Fail / [ ] Not Supported
- Notes: _______________________________________

---

## Debugging Commands

### Check MMS-Related Logs
```bash
# Watch MMS activity in real-time
adb -s 4A171JEBF12655 logcat | grep -iE "mms|wap"

# Check for MMS send errors
adb -s 4A171JEBF12655 logcat | grep -iE "mms.*error|mms.*fail"

# Check telephony provider
adb -s 4A171JEBF12655 logcat | grep -i telephony
```

### Check MMS Database
```bash
# Dump MMS messages from system provider
adb -s 4A171JEBF12655 shell content query --uri content://mms

# Check MMS parts (attachments)
adb -s 4A171JEBF12655 shell content query --uri content://mms/part
```

### Check APN Settings
```bash
# Dump APN configuration
adb -s 4A171JEBF12655 shell content query --uri content://telephony/carriers
```

---

## Common MMS Issues & Solutions

### Issue 1: "Message not sent" Error

**Possible Causes:**
- No mobile data connection
- APN settings missing/incorrect
- Carrier MMS block (rare)
- Attachment too large

**Solutions:**
1. Enable mobile data (even if on Wi-Fi)
2. Check APN settings have MMS APN entry
3. Try smaller attachment
4. Restart device

---

### Issue 2: MMS Receives as "Download" Button

**Possible Causes:**
- Mobile data disabled when MMS arrived
- APN MMS server unreachable
- Carrier issue

**Solutions:**
1. Enable mobile data
2. Tap "Download" button
3. Check APN settings

---

### Issue 3: Image/Video Not Displaying

**Possible Causes:**
- Corrupted download
- Unsupported media format
- Missing storage permission

**Solutions:**
1. Check storage permission
2. Re-download MMS
3. Check file format (JPEG/PNG/MP4 should work)

---

## Results Summary Template

**Test Date:** _______________
**Device:** Pixel 9a / GrapheneOS
**Carrier:** _______________
**Junction Version:** 1.0.0

| Test Case | Result | Notes |
|-----------|--------|-------|
| 1. MMS Send - Single Image | ⏳ | |
| 2. MMS Send - Large Image | ⏳ | |
| 3. MMS Send - Video | ⏳ | |
| 4. MMS Send - Audio | ⏳ | |
| 5. MMS Receive - Image | ⏳ | |
| 6. MMS Receive - Video | ⏳ | |
| 7. Group MMS | ⏳ | |
| 8. MMS over Wi-Fi Only | ⏳ | |

**Overall Status:** ⏳ Not Started / 🟡 Partial / ✅ Pass / ❌ Fail

**Critical Issues Found:**
- _______________________________________

**Recommendations:**
- _______________________________________

---

## Next Steps Based on Results

### If All Tests Pass ✅
1. Document MMS as fully functional
2. Update REMAINING_WORK.md status to ✅
3. Proceed to **Priority 1.2: Matrix Media Handling**
4. Use MMS as reference for Matrix media implementation

### If Some Tests Fail 🟡
1. Document which MMS features work vs broken
2. Prioritize fixes for critical features (image send/receive)
3. Decide if partial MMS is acceptable for MVP
4. Fix critical issues before Matrix media work

### If All Tests Fail ❌
1. Check AOSP MMS code for bugs introduced during fork
2. Review PATCHES.md for MMS-related changes
3. Compare with stock AOSP Messaging behavior
4. Consider this a blocker for Matrix media work

---

## Testing Procedure

**Recommended Order:**
1. Start with Test 1 (single image send) - most common use case
2. If Test 1 passes, proceed with Test 5 (image receive)
3. If both pass, MMS basics work → proceed with other tests
4. If Test 1 fails, start debugging before continuing

**Time Estimate:** 1-2 hours for full test suite

**Documentation:** Record results in this file or create `MMS_TEST_RESULTS.md`
