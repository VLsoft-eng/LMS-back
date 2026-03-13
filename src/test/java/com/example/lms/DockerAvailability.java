package com.example.lms;

import org.testcontainers.DockerClientFactory;

/**
 * Utility to check if Docker is available for Testcontainers.
 * Used to skip integration tests when Docker is not running.
 */
public final class DockerAvailability {

    private DockerAvailability() {}

    /**
     * Returns true if Docker is available and Testcontainers can use it.
     */
    public static boolean isAvailable() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
