package edu.virginia.lib.aptrust.bags;

import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.domain.Metadata;

import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * A class that encapsulates the information needed for the baginfo-info.txt
 * file that is included in an Bagit bag that is suitable for submission into AP Trust.
 */
public class BagInfo {

    private String sourceOrganization;
    private String baggingDate;
    private int bagNumber = 1;
    private int bagCount = 1;
    private String bagGroupIdentifier;
    private String internalSenderDescription;
    private String internalSenderIdentifier;

    public BagInfo() {
        this.sourceOrganization = System.getProperty("bagger.source-organization");
    }

    public BagInfo sourceOrganization(String source) {
        sourceOrganization = source;
        return this;
    }

    public String getSourceOrganization() {
        return sourceOrganization;
    }

    /**
     * Gets the bagging date, rendered as an ISO 8601 UTC date (to the milisecond).
     * This date is set to the current date the first time this method is called
     * and all subsequent calls return the same value.
     */
    public String getBaggingDate() {
        if (baggingDate == null) {
            baggingDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SZ").format(new Date());
        }
        return baggingDate;
    }

    public BagInfo bagNumber(int number) {
        this.bagNumber = number;
        return this;
    }

    public int getBagNumber() {
        return bagNumber;
    }

    public BagInfo bagCount(int count) {
        this.bagCount = count;
        return this;
    }

    public String getBagCount() {
        return bagNumber + " of " + bagCount;
    }

    public BagInfo bagGroupIdentifier(String id) {
        bagGroupIdentifier = id;
        return this;
    }

    public String getBagGroupIdentifier() {
        return bagGroupIdentifier;
    }

    public BagInfo internalSenderDescription(String desc) {
        internalSenderDescription = desc;
        return this;
    }

    public String getInternalSenderDescription() {
        return internalSenderDescription;
    }

    public BagInfo internalSenderIdentifier(String id) {
        internalSenderIdentifier = id;
        return this;
    }

    public String getInternalSenderIdentifier() {
        return internalSenderIdentifier;
    }

    /**
     * A convenience method to help serialize this objects as a BagInfo TXT file.
     */
    public void addToMetadata(Bag b) {
        final Metadata metadata = b.getMetadata();
        if (getSourceOrganization() != null) metadata.add("Source-Organization", getSourceOrganization());
        if (getBaggingDate() != null) metadata.add("Bagging-Date", getBaggingDate());
        metadata.add("Bag-Count", String.valueOf(getBagCount()));
        if (getBagGroupIdentifier() != null) metadata.add("Bag-Group-Identifier", getBagGroupIdentifier());
        if (getInternalSenderDescription() != null) metadata.add("Internal-Sender-Description", getInternalSenderDescription());
        if (getInternalSenderIdentifier() != null) metadata.add("Internal-Sender-Identifier", getInternalSenderIdentifier());
    }

}
