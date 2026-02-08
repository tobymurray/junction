# AOSP Chips Library

## Source

This directory contains the vendored AOSP chips library for contact chip UI components.

- **Upstream Source**: https://github.com/klinker41/android-chips
- **Based on**: Google's internal AOSP chips library (frameworks/ex/chips)
- **License**: Apache License 2.0
- **Date Vendored**: 2026-02-08

## About

The chips library provides RecipientEditTextView and related components for displaying and managing contact chips in auto-complete text fields. This is the same library used in AOSP Messaging.

## Modifications for Build Compatibility

The following minimal changes were made to integrate with the sms-upstream module:

### Package/Import Changes
- Added `import com.android.messaging.R;` to files referencing R resources:
  - ColorUtils.java
  - ContactImageCreator.java
  - DropdownChipLayouter.java
  - RecipientEditTextView.java

### AndroidX Migration
- Changed `android.support.annotation.*` to `androidx.annotation.*`:
  - ColorUtils.java: `@ColorInt`, `@ColorRes`
  - RecipientEditTextView.java: `@IdRes`

### Visibility Changes (for Messaging app integration)
- BaseRecipientAdapter:
  - `clearTempEntries()`: `private` → `protected`
  - `updateEntries()`: `private` → `protected`
  - `mCurrentConstraint`: `private` → `protected`
  - Added `mPhotoManager` field and `setPhotoManager()` method
- DropdownChipLayouter:
  - Added `getStyledResults()` helper method
- RecipientAlternatesAdapter:
  - `MAX_LOOKUPS`: package-private → `public`
- RecipientEntry:
  - `INVALID_DESTINATION_TYPE`: package-private → `public`
  - Main constructor: `private` → `protected` (for BugleRecipientEntry subclass)
- RecipientEditTextView:
  - `setDropdownChipLayouter()`: `protected` → `public`
  - Added `appendRecipientEntry()` and `removeRecipientEntry()` compatibility methods

### Custom Attributes (Messaging app extensions)
Added to RecipientEditTextView styleable:
- `unselectedChipTextColor` - Color for unselected chip text
- `unselectedChipBackgroundColor` - Background color for unselected chips

These attributes are used by the Messaging app layouts but are not part of the upstream AOSP chips library.

### Additional Files
- **PhotoManager.java**: Interface for photo loading (required by RecipientEditTextView but not included in original chips library)

## Resources

The following resources were added from the chips library:
- Layouts: `chips_alternate_item.xml`, `chips_recipient_dropdown_item.xml`, `copy_chip_dialog_layout.xml`, `more_item.xml`
- Drawables: Chip backgrounds, delete icons, contact picture placeholder (hdpi, xhdpi, xxhdpi)
- Dimensions: Chip sizing and spacing values
- Strings: UI strings for chip interactions
- Styles: RecipientEditTextView, ChipTitleStyle, ChipSubtitleStyle
- Attributes: RecipientEditTextView custom attributes
- Colors: Material Design color palette

## Maintenance

**IMPORTANT**: This directory contains vendored AOSP code. Avoid modifications except for minimal build compatibility changes as documented above.

When updating from upstream:
1. Replace all files with new upstream versions
2. Re-apply the modifications listed above
3. Update this README with the new upstream commit/tag
4. Test thoroughly with the Messaging app integration

## Integration Points

The chips library is used by:
- `ContactRecipientAutoCompleteView` (extends RecipientEditTextView)
- `ContactRecipientAdapter` (extends BaseRecipientAdapter)
- `ContactDropdownLayouter` (extends DropdownChipLayouter)
- `ContactRecipientPhotoManager` (implements PhotoManager)
