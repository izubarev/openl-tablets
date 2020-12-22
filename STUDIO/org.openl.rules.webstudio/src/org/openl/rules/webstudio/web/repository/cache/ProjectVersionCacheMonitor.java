package org.openl.rules.webstudio.web.repository.cache;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.openl.rules.common.ProjectVersion;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

public class ProjectVersionCacheMonitor implements Runnable, InitializingBean {

    private final Logger log = LoggerFactory.getLogger(ProjectVersionCacheMonitor.class);

    private ScheduledExecutorService scheduledPool;
    private ScheduledFuture<?> scheduled;
    private ProjectVersionH2CacheDB projectVersionCacheDB;
    private ProjectVersionCacheManager projectVersionCacheManager;
    private DesignTimeRepository designRepository;

    private final static int PERIOD = 10;

    @Override
    public void run() {
        try {
            if (!projectVersionCacheManager.isCacheCalculated()) {
                recalculateDesignRepositoryCache();
            }
        } catch (Exception e) {
            log.error("Error during project caching", e);
        }
    }

    private void recalculateDesignRepositoryCache() throws IOException {
        Collection<? extends AProject> projects = designRepository.getProjects();
        for (AProject project : projects) {
            if (project.isDeleted()) {
                continue;
            }
            cacheDesignProject(project);
            Thread.yield();
        }
        projectVersionCacheDB.setCacheCalculatedState(true);
    }

    private void cacheDesignProject(AProject project) throws IOException {
        Repository repository = designRepository.getRepository(project.getRepository().getId());
        List<ProjectVersion> versions = project.getVersions();
        if (repository.supports().branches()) {
            for (String branch : ((BranchRepository) repository).getBranches(project.getFolderPath())) {
                versions.addAll(new AProject(((BranchRepository) repository).forBranch(branch), project.getFolderPath())
                    .getVersions());
            }
        } else {
            versions.addAll(project.getVersions());
        }
        versions.sort((ProjectVersion pr1, ProjectVersion pr2) -> pr2.getVersionInfo()
            .getCreatedAt()
            .compareTo(pr1.getVersionInfo().getCreatedAt()));
        for (ProjectVersion projectVersion : versions) {
            if (projectVersion.isDeleted()) {
                continue;
            }
            String hash = projectVersionCacheDB.getHash(project.getBusinessName(),
                projectVersion.getVersionName(),
                projectVersion.getVersionInfo().getCreatedAt(),
                ProjectVersionH2CacheDB.RepoType.DESIGN);
            if (StringUtils.isEmpty(hash)) {
                Repository repo = project.getRepository();
                String branch = repo.supports().branches() ? ((BranchRepository) repo).getBranch() : null;
                AProject designProject = designRepository.getProjectByPath(project.getRepository().getId(),
                    branch,
                    project.getRealPath(),
                    projectVersion.getVersionName());
                cacheProjectVersion(designProject, ProjectVersionH2CacheDB.RepoType.DESIGN);
            }
        }
    }

    void cacheProjectVersion(AProject project, ProjectVersionH2CacheDB.RepoType repoType) throws IOException {
        String md5 = projectVersionCacheManager.computeMD5(project);
        projectVersionCacheDB.insertProject(project.getBusinessName(), project.getVersion(), md5, repoType);
    }

    public void setProjectVersionCacheDB(ProjectVersionH2CacheDB projectVersionCacheDB) {
        release();
        this.projectVersionCacheDB = projectVersionCacheDB;
    }

    public void setProjectVersionCacheManager(ProjectVersionCacheManager projectVersionCacheManager) {
        release();
        this.projectVersionCacheManager = projectVersionCacheManager;
    }

    public void setDesignRepository(DesignTimeRepository designRepository) {
        release();
        this.designRepository = designRepository;
    }

    @Override
    public void afterPropertiesSet() {
        if (projectVersionCacheDB != null && projectVersionCacheManager != null && designRepository != null) {
            scheduledPool = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = Executors.defaultThreadFactory().newThread(r);
                t.setDaemon(true);
                return t;
            });
            scheduled = scheduledPool.scheduleWithFixedDelay(this, 1, PERIOD, TimeUnit.SECONDS);
        }
    }

    public synchronized void release() {
        if (scheduledPool != null) {
            scheduledPool.shutdownNow();
        }
        if (scheduled != null) {
            scheduled.cancel(true);
            scheduled = null;
        }
        if (scheduledPool != null) {
            try {
                scheduledPool.awaitTermination(PERIOD, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
            scheduledPool = null;
        }
    }
}
