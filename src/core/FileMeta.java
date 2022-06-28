/*
 * Name: Sri Harish Pinnimti
 * UTA ID: 1001865949
 * Net ID: sxp5949
 *
 */
package core;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public class FileMeta implements Comparable<FileMeta>, Serializable {

    /** Name of the file */
    public final String name;
    /** Size of the file in bytes */
    public final long length;
    /** Last modified epoch in milliseconds */
    public final long lastModifiedEpoch;
    /** Last modified date */
    public final LocalDate lastModifiedDate;
    /** Serializable version ID */
    public static final long serialVersionUID = 1L;


    /**
     * Custom file object for Server files
     * @param name Name of the file
     * @param length Size of the file in bytes
     * @param lastModifiedEpoch Last modified epoch in milliseconds
     */
    public FileMeta(String name, long length, long lastModifiedEpoch) {
        this.name = name;
        this.length = length;
        this.lastModifiedEpoch = lastModifiedEpoch;
        this.lastModifiedDate = Instant.ofEpochMilli(this.lastModifiedEpoch)
                .atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * Compares two ServerFile objects based on their names
     * @param s ServerFile object to compare
     * @return Integer value to indicate whether the first object should be first
     */
    public int compareTo(FileMeta s) {
        return this.name.compareTo(s.name);
    }

    public boolean equals(FileMeta s) {
        return this.name.equals(s.name) && this.length == s.length && this.lastModifiedEpoch == s.lastModifiedEpoch;
    }

    public int hashCode() {
        return (int) (this.name.hashCode() ^ this.lastModifiedEpoch ^ this.length);
    }

    @Override
    public String toString() {
        return this.name + "\t\t" + this.length + "B" + "\t\t" + this.lastModifiedDate;
    }

}
