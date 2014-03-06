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
    final private String rights;

    public APTrustInfo(String title, String rights) {
        if (CONSORTIA.equals(rights) || RESTRICTED.equals(rights) || INSTITUTION.equals(rights)) {
            this.rights = rights;
        } else {
            throw new IllegalArgumentException("Illegal \"rights\" value: \"" + rights + "\"");
        }
        this.title = title;
    }

    public String getTitle() {
        return this.title;
    }

    public String getRights() {
        return this.rights;
    }

}
