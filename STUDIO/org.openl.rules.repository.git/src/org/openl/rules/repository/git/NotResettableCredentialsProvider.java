package org.openl.rules.repository.git;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotResettableCredentialsProvider extends UsernamePasswordCredentialsProvider {
    private static final Logger LOG = LoggerFactory.getLogger(NotResettableCredentialsProvider.class);

    private final int failedAuthorizationSeconds;
    private final int maxAuthorizationAttempts;
    private final String repositoryName;

    //When authentication attempt was unsuccessful the next authentication attempt does not occur immediately,
    //but after the time specified in the properties.
    private AtomicLong nextAttempt = new AtomicLong(0);
    private AtomicInteger attemptNumber = new AtomicInteger(0);


    NotResettableCredentialsProvider(String username, String password, String repositoryName, int failedAuthorizationSeconds, int maxAuthorizationAttempts) {
        super(username, password);
        this.repositoryName = repositoryName;
        this.failedAuthorizationSeconds = failedAuthorizationSeconds;
        this.maxAuthorizationAttempts = maxAuthorizationAttempts;
    }

    @Override
    public void reset(URIish uri) {
        // This method is called when authentication attempt was unsuccessful and need to provide correct credentials.
        // Our application works in non-interactive mode so we just throw exception.
        LOG.info("Reset the credentials provider for the URI: {}", uri);
        if (!nextAttempt.compareAndSet(0, System.currentTimeMillis() + failedAuthorizationSeconds * 1000)) {
            // The following condition will be false only in case of a simultaneous request to the repository by several threads
            // It is necessary to increase the attempt counter so that the total number of attempts does not exceed the maximum number of attempts.
            if (!attemptNumber.compareAndSet(0, 1)) {
                //Means that the one more authentication attempt has failed
                nextAttempt.set(System.currentTimeMillis() + failedAuthorizationSeconds * 1000);
            }
        }
        throw new InvalidCredentialsException(String.format("Problem communicating with '%s' Git server, will retry automatically in %s", repositoryName, getNextAttemptTime()));
    }

    void validateAuthorizationState() throws IOException {
        long attemptTime = nextAttempt.get();
        if (attemptTime == 0) {
            // The last login attempt was successful, or this is the first attempt.
            return;
        } else if (attemptTime == -1) {
            // The maximum number of authorization attempts has been exceeded.
            throw new IOException("Incorrect login or password for git repository.");
        } else {
            if (System.currentTimeMillis() > attemptTime) {
                if (attemptNumber.incrementAndGet() <= maxAuthorizationAttempts) {
                    // Increase in the counter of attempts and permission for one more.
                    return;
                } else {
                    // The maximum number of authorization attempts has been exceeded. No more attempts allowed.
                    nextAttempt.set(-1);
                    throw new IOException("Incorrect login or password for git repository.");
                }
            } else {
                // The time for the next try has not yet come
                throw new IOException(String.format("Problem communicating with '%s' Git server, will retry automatically in %s", repositoryName, getNextAttemptTime()));
            }
        }
    }

    private String getNextAttemptTime() {
        int nextAttemptTimeInSeconds = (int) (nextAttempt.get() - System.currentTimeMillis()) / 1000;
        int minutes = nextAttemptTimeInSeconds / 60;
        if (minutes != 0) {
            return minutes + " minute(s).";
        } else {
            return nextAttemptTimeInSeconds + " second(s).";
        }
    }

    void successAuthentication() {
        nextAttempt = new AtomicLong(0);
        attemptNumber = new AtomicInteger(0);
    }


    @Override
    public void clear() {
        // Do nothing to ensure that username and password isn't cleared.
        LOG.warn("clear() method should never be invoked.");
    }
}
