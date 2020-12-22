package org.openl.rules.common.impl;

import java.util.regex.Pattern;

import org.openl.rules.common.CommonVersion;

public class CommonVersionImpl implements CommonVersion {
    private static final Pattern ONLY_DIGITS = Pattern.compile("\\d+");
    private static final String VERSION_PATTERN = "\\d+\\.\\d+(\\.\\d+.*)?";
    private int major = MAX_MM_INT;
    private int minor = MAX_MM_INT;
    private String revision = "0";

    private transient String versionName;

    public CommonVersionImpl(CommonVersion version) {
        major = version.getMajor();
        minor = version.getMinor();
        revision = version.getRevision();
    }

    public CommonVersionImpl(int major, int minor, int revision) {
        this.major = major;
        this.minor = minor;
        this.revision = "" + revision;
    }

    public CommonVersionImpl(int revision) {
        this.revision = "" + revision;
    }

    /**
     * x -> revision x.y -> major.minor x.y.z -> major.minor.revision
     */
    public CommonVersionImpl(String s) {
        if (!s.matches(VERSION_PATTERN)) {
            revision = s;
            return;
        }
        String[] version = s.split("\\.");

        if (version.length == 1) {
            revision = version[0];
        } else {
            major = Integer.parseInt(version[0]);
            minor = Integer.parseInt(version[1]);
            if (version.length > 2) {
                revision = version[2];
            }
        }
    }

    @Override
    public int compareTo(CommonVersion o) {
        /* Version with the same Revisions always equal */
        if (revision.equals(o.getRevision())) {
            return 0;
        }

        /* Revision with num 0 always should be at last place */
        if ("0".equals(revision)) {
            return -1;
        } else if ("0".equals(o.getRevision())) {
            return 1;
        }

        if (major != o.getMajor()) {
            return major < o.getMajor() ? -1 : 1;
        }

        if (minor != o.getMinor()) {
            return minor < o.getMinor() ? -1 : 1;
        }

        return compareRevision(o);
    }

    private int compareRevision(CommonVersion other) {
        String otherRevision = other.getRevision();

        if (ONLY_DIGITS.matcher(revision).matches() && ONLY_DIGITS.matcher(otherRevision).matches()) {
            return Integer.parseInt(revision) - Integer.parseInt(otherRevision);
        }

        // Cannot be parsed as int. Compare as Strings.
        return revision.compareTo(otherRevision);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CommonVersion)) {
            return false;
        }

        return compareTo((CommonVersion) o) == 0;
    }

    @Override
    public int getMajor() {
        return major;
    }

    @Override
    public int getMinor() {
        return minor;
    }

    @Override
    public String getRevision() {
        return revision;
    }

    @Override
    public String getVersionName() {
        if (versionName == null) {
            if (major != MAX_MM_INT && minor != MAX_MM_INT && major != -1 && minor != -1) {
                versionName = major + "." + minor + "." + revision;
            } else {
                versionName = revision;
            }
        }

        return versionName;
    }

    @Override
    public String getShortVersion() {
        String version = getVersionName();
        return version == null || version.length() < 6 ? version : version.substring(0, 6);
    }

    @Override
    public int hashCode() {
        return major << 22 ^ minor << 11 ^ revision.hashCode();
    }

    @Override
    public String toString() {
        return "versionName = " + getVersionName();
    }
}
