package org.openl.rules.repository.file;

import java.util.function.Function;

import org.openl.rules.repository.RepositoryFactory;
import org.openl.rules.repository.RepositoryInstatiator;
import org.openl.rules.repository.api.Repository;

/**
 * Local file system repository factory.
 *
 * @author Yury Molchan
 */
public class LocalRepositoryFactory implements RepositoryFactory {
    private static final String ID = "repo-file";
    private static final String OLD_ID = "org.openl.rules.repository.LocalRepositoryFactory";

    @Override
    public boolean accept(String factoryID) {
        return ID.equals(factoryID) || OLD_ID.equals(factoryID);
    }

    @Override
    public String getRefID() {
        return ID;
    }

    @Override
    public Repository create(Function<String, String> settings) {
        LocalRepository repository = new LocalRepository();
        RepositoryInstatiator.setParams(repository, settings);
        repository.initialize();
        return repository;
    }
}
