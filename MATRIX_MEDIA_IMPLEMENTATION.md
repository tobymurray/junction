# Matrix Media Handling Implementation Plan

**Priority:** 1.2 - CRITICAL
**Estimated Effort:** 16 hours
**Status:** 🟡 In Progress

---

## Overview

Implement bidirectional media handling between SMS MMS ↔ Matrix, enabling images, videos, and audio to flow through the bridge.

---

## Current State

### Existing Code
- **File:** `TrixnityMatrixBridge.kt:122-143`
- **Status:** TODO stubs with implementation patterns
- **Data Structure:** `MatrixAttachment(uri, mimeType, filename, size)`

### API Available (Trixnity 5.1.0)
```kotlin
// Media upload
client.media.prepareUploadMedia(content: ByteArray, contentType: String): Result<Uri>
client.media.uploadMedia(cacheUri: Uri): Result<MxcUrl>

// Media download
client.media.getMedia(mxcUri: MxcUrl): Result<ByteArray>

// Message sending with media
client.room.sendMessage(roomId) {
    image(mxcUrl, body = "filename.jpg", info = ImageInfo(...))
    video(mxcUrl, body = "video.mp4", info = VideoInfo(...))
    audio(mxcUrl, body = "audio.m4a", info = AudioInfo(...))
    file(mxcUrl, body = "file.pdf", info = FileInfo(...))
}
```

---

## Implementation Tasks

### Phase 1: SMS MMS → Matrix (Outbound)

#### Task 1.1: Implement Image Upload ✅ (Next)
**File:** `TrixnityMatrixBridge.kt:125-132`

**Steps:**
1. Read image file from URI using ContentResolver
2. Prepare upload with Trixnity `prepareUploadMedia()`
3. Upload to Matrix with `uploadMedia()`
4. Extract image dimensions (width, height) for ImageInfo
5. Send message with `image()` DSL including metadata

**Code Pattern:**
```kotlin
attachment.mimeType.startsWith("image/") -> {
    val content = readFileFromUri(Uri.parse(attachment.uri))
    val cacheUri = client.media.prepareUploadMedia(content, attachment.mimeType).getOrThrow()
    val mxcUri = client.media.uploadMedia(cacheUri).getOrThrow()

    val imageInfo = ImageInfo(
        height = extractHeight(content),
        width = extractWidth(content),
        mimeType = attachment.mimeType,
        size = attachment.size
    )

    client.room.sendMessage(RoomId(roomIdStr)) {
        image(
            url = mxcUri,
            body = attachment.filename ?: "image.jpg",
            info = imageInfo
        )
    }
}
```

#### Task 1.2: Implement Video Upload
**Similar to image but with VideoInfo:**
- Extract video dimensions
- Extract duration if possible
- Use `video()` DSL

#### Task 1.3: Implement Audio Upload
**Similar to video but with AudioInfo:**
- Extract duration
- Use `audio()` DSL

#### Task 1.4: Generic File Upload
**For other MIME types:**
- Use `file()` DSL
- Minimal metadata required

---

### Phase 2: Matrix → SMS MMS (Inbound)

Currently, `observeMatrixMessages()` subscribes to timeline events and filters for text messages. Need to extend to handle media.

#### Task 2.1: Detect Media Messages
**File:** `TrixnityMatrixBridge.kt:184-260`

**Current Code (line 213-229):**
```kotlin
when (val messageContent = content) {
    is RoomMessageEventContent.TextBased.Text -> {
        // Extract phone number from room
        val phoneNumber = roomMapper.getPhoneNumber(roomId.full) ?: return@collect

        // Emit text message
        _inboundMessages.emit(
            MatrixInboundMessage(
                phoneNumber = phoneNumber,
                message = messageContent.body,
                timestamp = event.originTimestamp.value
            )
        )
    }
    else -> {
        // Ignore other content types for now
    }
}
```

**Extend to handle:**
- `RoomMessageEventContent.ImageBased.Image`
- `RoomMessageEventContent.VideoBased.Video`
- `RoomMessageEventContent.AudioBased.Audio`
- `RoomMessageEventContent.FileBased.File`

#### Task 2.2: Download Media from Matrix
**Add helper function:**
```kotlin
private suspend fun downloadMatrixMedia(mxcUrl: MxcUrl): ByteArray? {
    return try {
        client.media.getMedia(mxcUrl).getOrNull()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
```

#### Task 2.3: Store Media Locally
**Steps:**
1. Download media bytes from Matrix
2. Determine appropriate file extension from MIME type
3. Store in app's cache directory or media directory
4. Return local file URI for MMS sending

**Helper function:**
```kotlin
private fun saveMediaToCache(
    content: ByteArray,
    mimeType: String,
    filename: String
): Uri {
    val extension = MimeTypeMap.getSingleton()
        .getExtensionFromMimeType(mimeType) ?: "bin"
    val file = File(context.cacheDir, "$filename.$extension")
    file.writeBytes(content)
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}
```

#### Task 2.4: Update MatrixInboundMessage
**Current:**
```kotlin
data class MatrixInboundMessage(
    val phoneNumber: String,
    val message: String,
    val timestamp: Long
)
```

**Update to:**
```kotlin
data class MatrixInboundMessage(
    val phoneNumber: String,
    val message: String,
    val timestamp: Long,
    val attachments: List<MatrixInboundAttachment> = emptyList()
)

data class MatrixInboundAttachment(
    val localUri: Uri,  // Local file URI after download
    val mimeType: String,
    val filename: String,
    val size: Long
)
```

---

### Phase 3: Compression & Size Limits

MMS has carrier-imposed size limits (typically 300KB-600KB). Matrix has no such limit.

#### Task 3.1: Image Compression (MMS → Matrix)
**Not critical for MVP:**
- Matrix can handle large images
- Compress only if performance becomes an issue

#### Task 3.2: Image Compression (Matrix → MMS) **CRITICAL**
**Must compress before sending MMS:**
```kotlin
private fun compressImageForMms(bytes: ByteArray, maxSize: Long = 300_000): ByteArray {
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    val stream = ByteArrayOutputStream()
    var quality = 90

    do {
        stream.reset()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        quality -= 10
    } while (stream.size() > maxSize && quality > 10)

    return stream.toByteArray()
}
```

#### Task 3.3: Video Transcoding
**Complex - defer to Phase 4:**
- Video transcoding is CPU-intensive
- May require FFmpeg or MediaCodec
- For MVP: Reject videos > MMS limit with error message

---

### Phase 4: FileProvider Configuration

Need to expose cached media files to MMS system via FileProvider.

#### Task 4.1: Add FileProvider to Manifest
**File:** `app/src/main/AndroidManifest.xml`

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

#### Task 4.2: Create file_paths.xml
**File:** `app/src/main/res/xml/file_paths.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <cache-path name="matrix_media" path="matrix/" />
</paths>
```

---

## Testing Strategy

### Unit Tests
- Test URI reading (mock ContentResolver)
- Test MIME type detection
- Test image dimension extraction
- Test compression logic

### Integration Tests
1. **MMS → Matrix Image:**
   - Send MMS with image from phone
   - Verify image appears in Matrix room
   - Check image quality and size

2. **Matrix → MMS Image:**
   - Send image in Matrix room
   - Verify MMS arrives with attachment
   - Check compression applied correctly

3. **Video/Audio:**
   - Same tests as image
   - Verify playback works

### Manual Testing Checklist
- [ ] Send photo via MMS → appears in Matrix
- [ ] Send photo in Matrix → arrives as MMS
- [ ] Send video via MMS → appears in Matrix
- [ ] Send video in Matrix → arrives as MMS (or error if too large)
- [ ] Send audio via MMS → appears in Matrix
- [ ] Send audio in Matrix → arrives as MMS
- [ ] Large image (> 600KB) Matrix → MMS: compressed correctly
- [ ] Very large image (> 5MB) Matrix → MMS: handled gracefully

---

## Error Handling

### Scenarios to Handle:
1. **File read error:** URI invalid or permission denied
2. **Upload failure:** Network error, Matrix server down
3. **Download failure:** MXC URI invalid, file deleted
4. **Compression failure:** Image too large even after compression
5. **MMS send failure:** Same as text MMS (no cellular, etc.)

### Error Messages:
```kotlin
sealed class MediaBridgeError {
    object FILE_READ_FAILED : MediaBridgeError()
    object UPLOAD_FAILED : MediaBridgeError()
    object DOWNLOAD_FAILED : MediaBridgeError()
    object COMPRESSION_FAILED : MediaBridgeError()
    object FILE_TOO_LARGE : MediaBridgeError()
}
```

---

## Dependencies

### Already Available:
- ✅ Trixnity 5.1.0 media APIs
- ✅ Android ContentResolver
- ✅ Android BitmapFactory

### Need to Add:
- FileProvider (androidx.core)
- MimeTypeMap (Android SDK - already available)

---

## Success Criteria

### MVP (Phase 1 + 2):
- [x] Read MMS attachments from URI
- [ ] Upload images to Matrix
- [ ] Upload videos to Matrix
- [ ] Upload audio to Matrix
- [ ] Detect Matrix media messages
- [ ] Download media from Matrix
- [ ] Convert Matrix media to MMS attachments
- [ ] Send MMS with downloaded media

### Production (Phase 3):
- [ ] Image compression for MMS size limits
- [ ] Video transcoding or rejection
- [ ] Graceful error handling

### Polish (Phase 4):
- [ ] Progress indicators for uploads/downloads
- [ ] Thumbnail generation
- [ ] Media caching strategy

---

## Estimated Timeline

| Phase | Tasks | Hours | Status |
|-------|-------|-------|--------|
| Phase 1 | MMS → Matrix upload | 6h | ⏳ Next |
| Phase 2 | Matrix → MMS download | 6h | ⏳ |
| Phase 3 | Compression | 3h | ⏳ |
| Phase 4 | FileProvider | 1h | ⏳ |
| **Total** | | **16h** | |

---

## Next Immediate Step

**Start with Phase 1, Task 1.1: Implement Image Upload**

1. Add helper function `readFileFromUri()`
2. Add helper function `extractImageDimensions()`
3. Implement image upload in `sendMmsToMatrix()`
4. Test with actual MMS image attachment
