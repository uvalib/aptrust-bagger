package edu.virginia.lib.aptrust.bags;

/**
 * An immutable class representing the information needed for the aptrust-info.txt
 * file that is included in an Bagit bag that is suitable for submission into AP Trust.
 */
public class APTrustInfo {

    public static final String CONSORTIA = "Consortia";
    public static final String RESTRICTED = "Restricted";
    public static final String INSTITUTION = "Institution";

    final private String title;
    final private String access;

    public APTrustInfo(String title, String access) {
        if (CONSORTIA.equals(access) || RESTRICTED.equals(access) || INSTITUTION.equals(access)) {
            this.access = access;
        } else {
            throw new IllegalArgumentException("Illegal \"access\" value: \"" + access + "\"");
        }
        this.title = title;
    }

    public String getTitle() {
        return this.title;
    }

    public String getAccess() {
        return this.access;
    }

}
