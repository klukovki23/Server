package com.viikko5;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ObservationRecord {
    private String recordIdentifier;
    private String recordDescription;
    private String recordPayload;
    private String recordRightAscension;
    private String recordDeclination; 
    private String recordTimeReceived;
    private String recordOwner;
    private List<Observatory> observatory;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");

    
    public ObservationRecord(String recordIdentifier, String recordDescription, 
                             String recordPayload, String recordRightAscension, 
                             String recordDeclination, String recordOwner, List<Observatory> observatory) {
        this.recordIdentifier = recordIdentifier;
        this.recordDescription = recordDescription;
        this.recordPayload = recordPayload;
        this.recordRightAscension = recordRightAscension;
        this.recordDeclination = recordDeclination; 
        this.recordOwner = recordOwner;
        this.observatory = observatory;

        this.recordTimeReceived = ZonedDateTime.now(java.time.ZoneId.of("UTC")).format(formatter);
    }

    

    public String getRecordOwner() { return recordOwner; }
    public void setRecordOwner(String recordOwner) { this.recordOwner = recordOwner; }

    public List<Observatory> getObservatory() { return observatory; }
    public void setObservatories(List<Observatory> observatory) { this.observatory = observatory; }

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
        return  "recordIdentifier='" + recordIdentifier + '\'' +
                ", recordDescription='" + recordDescription + '\'' +
                ", recordPayload='" + recordPayload + '\'' +
                ", recordRightAscension='" + recordRightAscension + '\'' +
                ", recordDeclination='" + recordDeclination + '\'' +
                ", recordTimeReceived='" + recordTimeReceived + '\'' +
                ", recordOwner='" + recordOwner + '\'' +
                ", observatory=" + (observatory != null ? observatory.toString() : "[]") +
                '}';
    }
        
    
        
    
    public static class Observatory {

        private String observatoryName;
        private double latitude;
        private double  longitude;

        public Observatory(String observatoryName, double latitude, double longitude) {
            this.observatoryName = observatoryName;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public String getObservatoryName() { return observatoryName; }
        public void setObservatoryName(String observatoryName) { this.observatoryName = observatoryName; }

        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }

        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }

        @Override
        public String toString() {
            return "{" +
                    "observatoryName='" + observatoryName + '\'' +
                    ", latitude=" + latitude +
                    ", longitude=" + longitude +
                    '}';
        }
    }
}
