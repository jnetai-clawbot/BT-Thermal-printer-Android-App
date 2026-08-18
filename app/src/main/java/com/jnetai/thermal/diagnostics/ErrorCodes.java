package com.jnetai.thermal.diagnostics;

public final class ErrorCodes {
    private static final String PREFIX = "JNT-";

    public static final String BT_001 = PREFIX + "BT-001"; // Bluetooth adapter unavailable
    public static final String BT_002 = PREFIX + "BT-002"; // Bluetooth permission missing
    public static final String BT_003 = PREFIX + "BT-003"; // No paired printers found
    public static final String BT_004 = PREFIX + "BT-004"; // Connection failure
    public static final String BT_005 = PREFIX + "BT-005"; // Socket closed / IO during write
    public static final String BT_006 = PREFIX + "BT-006"; // Device not found
    public static final String BT_007 = PREFIX + "BT-007"; // Discovery failure

    public static final String PR_001 = PREFIX + "PR-001"; // Print job failed
    public static final String PR_002 = PREFIX + "PR-002"; // Connection not open
    public static final String PR_003 = PREFIX + "PR-003"; // Data encode/build error
    public static final String PR_004 = PREFIX + "PR-004"; // Printer test failure
    public static final String PR_005 = PREFIX + "PR-005"; // No printer selected for job
    public static final String PR_006 = PREFIX + "PR-006"; // Image encode failure

    public static final String ST_001 = PREFIX + "ST-001"; // Settings load error
    public static final String ST_002 = PREFIX + "ST-002"; // Settings save error

    public static final String TM_001 = PREFIX + "TM-001"; // Template load error
    public static final String TM_002 = PREFIX + "TM-002"; // Template save error
    public static final String TM_003 = PREFIX + "TM-003"; // Template delete error
    public static final String TM_004 = PREFIX + "TM-004"; // Template parse error

    public static final String RC_001 = PREFIX + "RC-001"; // Receipt save error
    public static final String RC_002 = PREFIX + "RC-002"; // Receipt load error
    public static final String RC_003 = PREFIX + "RC-003"; // Email intent error

    public static final String QR_001 = PREFIX + "QR-001"; // QR generate error
    public static final String QR_002 = PREFIX + "QR-002"; // QR encode / save error
    public static final String QR_003 = PREFIX + "QR-003"; // QR scan error
    public static final String QR_004 = PREFIX + "QR-004"; // QR parse error

    public static final String FL_001 = PREFIX + "FL-001"; // File read error
    public static final String FL_002 = PREFIX + "FL-002"; // File open error

    public static final String UP_001 = PREFIX + "UP-001"; // Update check network error
    public static final String UP_002 = PREFIX + "UP-002"; // Update check API error
    public static final String UP_003 = PREFIX + "UP-003"; // Update check parse error

    public static final String GE_001 = PREFIX + "GE-001"; // General unexpected error
    public static final String IM_001 = PREFIX + "IM-001"; // Image load/decode error
    public static final String SHR_001 = PREFIX + "SHR-001"; // Share/email intent failure

    private ErrorCodes() {}
}