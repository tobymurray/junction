/*
 * STUB: com.android.vcard.VCardProperty
 */
package com.android.vcard;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class VCardProperty {
    private String mName;
    private List<String> mGroupList;
    private Map<String, List<String>> mParameterMap;
    private String mRawValue;
    private byte[] mByteValue;

    public String getName() { return mName; }
    public void setName(String name) { mName = name; }
    public List<String> getGroupList() { return mGroupList != null ? mGroupList : new ArrayList<>(); }
    public Map<String, List<String>> getParameterMap() {
        return mParameterMap != null ? mParameterMap : new HashMap<>();
    }
    public List<String> getParameters(String type) {
        Map<String, List<String>> map = getParameterMap();
        return map.containsKey(type) ? map.get(type) : new ArrayList<>();
    }
    public String getRawValue() { return mRawValue; }
    public void setRawValue(String rawValue) { mRawValue = rawValue; }
    public byte[] getByteValue() { return mByteValue; }
}
