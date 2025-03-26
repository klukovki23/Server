package com.viikko4;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;


public class ObservationRecord {
    private String recordIdentifier;
    private String recordDescription;
    private String recordPayload;
    private String recordRightAscension;
    private String recordDeclination;
    private String recordTimeReceived;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");

    public ObservationRecord(String recordIdentifier, String recordDescription, 
                             String recordPayload, String recordRightAscension, 
                             String recordDeclination) {
        this.recordIdentifier = recordIdentifier;
        this.recordDescription = recordDescription;
        this.recordPayload = recordPayload;
        this.recordRightAscension = recordRightAscension;
        this.recordDeclination = recordDeclination;

         this.recordTimeReceived = ZonedDateTime.now(java.time.ZoneId.of("UTC")).format(formatter);
    
    }

    public String getRecordIdentifier() { return recordIdentifier; }
    public void setRecordIdentifier(String recordIdentifier) { this.recordIdentifier = recordIdentifier; }

    public String getRecordDescription() { return recordDescription; }
    public void setRecordDescription(String recordDescription) { this.recordDescription = recordDescription; }

    public String getRecordPayload() { return recordPayload; }
    public void setRecordPayload(String recordPayload) { this.recordPayload = recordPayload; }

    public String getRecordRightAscension() { return recordRightAscension; }
    public void setRecordRightAscension(String recordRightAscension) { this.recordRightAscension = recordRightAscension; }

    public String getRecordDeclination() { return recordDeclination; }
    public void setRecordDeclination(String recordDeclination) { this.recordDeclination = recordDeclination; }

    public String getRecordTimeReceived() { return recordTimeReceived; }
    public void setRecordTimeReceived(String recordTimeReceived) { this.recordTimeReceived = recordTimeReceived; }
    
    @Override
    public String toString() {
        return "ObservationRecord{" +
                "recordIdentifier='" + recordIdentifier + '\'' +
                ", recordDescription='" + recordDescription + '\'' +
                ", recordPayload='" + recordPayload + '\'' +
                ", recordRightAscension='" + recordRightAscension + '\'' +
                ", recordDeclination='" + recordDeclination + '\'' +
                ", recordTimeReceived='" + recordTimeReceived + '\'' +
                '}';
    }
}

