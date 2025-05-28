package org.akj.test.tracker.domain.common.util;

import lombok.extern.slf4j.Slf4j;
import org.akj.test.tracker.domain.rule.model.VersionOperator;

import java.util.Arrays;
import java.util.regex.Pattern;


@Slf4j
public class VersionComparator {
    private static final Pattern VERSION_PATTERN = Pattern.compile("^[0-9]+(\\.[0-9]+)*(-[a-zA-Z0-9]+)?$");

    public static boolean isVersionCompliant(String currentVersion, String targetVersion, VersionOperator operator) {
        log.info("Comparing versions: currentVersion={}, targetVersion={}, operator={}", currentVersion, targetVersion, operator.getSign());
        if (currentVersion == null || targetVersion == null) {
            log.warn("Version comparison failed: currentVersion={}, targetVersion={}", currentVersion, targetVersion);
            return false;
        }

        if (!isValidVersion(currentVersion) || !isValidVersion(targetVersion)) {
            log.warn("Invalid version format: currentVersion={}, targetVersion={}", currentVersion, targetVersion);
            return false;
        }

        // Remove any suffix for comparison
        String currentBase = removeSuffix(currentVersion);
        String targetBase = removeSuffix(targetVersion);

        int comparison = compareVersions(currentBase, targetBase);
        log.debug("Version comparison: current={}, target={}, operator={}, result={}",
                currentVersion, targetVersion, operator, comparison);

        boolean result = switch (operator) {
            case EQUAL -> comparison == 0;
            case GREATER -> comparison > 0;
            case GREATER_EQUAL -> comparison >= 0;
            case LESS -> comparison < 0;
            case LESS_EQUAL -> comparison <= 0;
            case TILDE -> isTildeCompliant(currentBase, targetBase);
            case CARET -> isCaretCompliant(currentBase, targetBase);
        };

        log.info("Version compliance result: currentVersion={}, targetVersion={}, operator={}, result={}",
                currentVersion, targetVersion, operator.getSign(), result);
        return result;
    }

    private static boolean isValidVersion(String version) {
        return VERSION_PATTERN.matcher(version).matches();
    }

    // ~: Allows patch-level changes if a minor version is specified,
    // or minor-level changes if not. E.g., ~1.2.3 := >=1.2.3 <1.3.0
    private static boolean isTildeCompliant(String current, String target) {
        String[] targetParts = target.split("\\.");
        String[] currentParts = current.split("\\.");

        if (targetParts.length < 2) {
            // ~1 := >=1.0.0 <2.0.0
            int targetMajor = Integer.parseInt(targetParts[0]);
            int currentMajor = Integer.parseInt(currentParts[0]);
            return currentMajor == targetMajor && compareVersions(current, target) >= 0;
        } else {
            // ~1.2 or ~1.2.3 := >=1.2.0 <1.3.0
            int targetMajor = Integer.parseInt(targetParts[0]);
            int targetMinor = Integer.parseInt(targetParts[1]);
            int currentMajor = Integer.parseInt(currentParts[0]);
            int currentMinor = Integer.parseInt(currentParts[1]);
            return currentMajor == targetMajor && currentMinor == targetMinor && compareVersions(current, target) >= 0;
        }
    }

    // ^: Allows changes that do not modify the left-most non-zero digit.
    // E.g., ^1.2.3 := >=1.2.3 <2.0.0, ^0.2.3 := >=0.2.3 <0.3.0, ^0.0.3 := >=0.0.3 <0.0.4
    private static boolean isCaretCompliant(String current, String target) {
        String[] targetParts = target.split("\\.");
        String[] currentParts = current.split("\\.");

        int major = Integer.parseInt(targetParts[0]);
        int currentMajor = Integer.parseInt(currentParts[0]);
        if (major > 0) {
            // ^1.x.x := >=1.0.0 <2.0.0
            return currentMajor == major && compareVersions(current, target) >= 0;
        } else if (targetParts.length > 1 && Integer.parseInt(targetParts[1]) > 0) {
            // ^0.2.x := >=0.2.0 <0.3.0
            int minor = Integer.parseInt(targetParts[1]);
            int currentMinor = Integer.parseInt(currentParts[1]);
            return currentMajor == 0 && currentMinor == minor && compareVersions(current, target) >= 0;
        } else if (targetParts.length > 2) {
            // ^0.0.3 := >=0.0.3 <0.0.4
            int patch = Integer.parseInt(targetParts[2]);
            int currentPatch = Integer.parseInt(currentParts[2]);
            return currentMajor == 0 && Integer.parseInt(currentParts[1]) == 0 && currentPatch == patch && compareVersions(current, target) >= 0;
        }
        return false;
    }

    private static String removeSuffix(String version) {
        int dashIndex = version.indexOf('-');
        return dashIndex > 0 ? version.substring(0, dashIndex) : version;
    }

    private static int compareVersions(String version1, String version2) {
        String[] v1Parts = version1.split("\\.");
        String[] v2Parts = version2.split("\\.");

        int maxLength = Math.max(v1Parts.length, v2Parts.length);
        v1Parts = padArray(v1Parts, maxLength);
        v2Parts = padArray(v2Parts, maxLength);

        for (int i = 0; i < maxLength; i++) {
            int v1 = Integer.parseInt(v1Parts[i]);
            int v2 = Integer.parseInt(v2Parts[i]);

            int comparison = Integer.compare(v1, v2);
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }

    private static String[] padArray(String[] array, int length) {
        if (array.length == length) {
            return array;
        }
        String[] padded = new String[length];
        Arrays.fill(padded, "0");
        System.arraycopy(array, 0, padded, 0, array.length);
        return padded;
    }
} 