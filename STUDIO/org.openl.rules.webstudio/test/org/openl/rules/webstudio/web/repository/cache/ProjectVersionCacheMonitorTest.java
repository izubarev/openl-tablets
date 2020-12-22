package org.openl.rules.webstudio.web.repository.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.git.GitRepositoryFactory;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;

public class ProjectVersionCacheMonitorTest {

    private File root;
    private Repository repo;
    private ProjectVersionCacheMonitor projectVersionCacheMonitor;
    private ProjectVersionCacheManager projectVersionCacheManager;
    private ProjectVersionH2CacheDB projectVersionCacheDB;

    @Before
    public void setUp() throws IOException {
        root = Files.createTempDirectory("openl").toFile();
        repo = createRepository(new File(root, "design-repository"));
        projectVersionCacheMonitor = new ProjectVersionCacheMonitor();
        projectVersionCacheManager = new ProjectVersionCacheManager();
        projectVersionCacheDB = new ProjectVersionH2CacheDB();
        projectVersionCacheDB.setOpenLHome(root.getAbsolutePath());
        projectVersionCacheMonitor.setProjectVersionCacheDB(projectVersionCacheDB);
        projectVersionCacheManager.setProjectVersionCacheDB(projectVersionCacheDB);
        projectVersionCacheMonitor.setProjectVersionCacheManager(projectVersionCacheManager);
    }

    @After
    public void tearDown() throws Exception {
        if (repo != null) {
            repo.close();
        }
        FileUtils.delete(root);
        if (root.exists()) {
            fail("Cannot delete folder " + root);
        }
    }

    @Test
    public void testCacheProjects() throws IOException {
        String path = "project/test";
        FileData data = repo.save(createFileData(path, path), IOUtils.toInputStream(path + "1"));
        FileData data2 = repo.save(createFileData(path, path), IOUtils.toInputStream(path + "2"));
        FileData data3 = repo.save(createFileData(path, path), IOUtils.toInputStream(path + "3"));
        AProject project = new AProject(repo, "project", data.getVersion());
        AProject project2 = new AProject(repo, "project", data2.getVersion());
        AProject project3 = new AProject(repo, "project", data3.getVersion());
        projectVersionCacheMonitor.cacheProjectVersion(project, ProjectVersionH2CacheDB.RepoType.DESIGN);
        projectVersionCacheMonitor.cacheProjectVersion(project2, ProjectVersionH2CacheDB.RepoType.DESIGN);
        projectVersionCacheMonitor.cacheProjectVersion(project2, ProjectVersionH2CacheDB.RepoType.DEPLOY);
        projectVersionCacheMonitor.cacheProjectVersion(project3, ProjectVersionH2CacheDB.RepoType.DESIGN);
        String deployedProjectVersion = projectVersionCacheManager.getDeployedProjectVersion(project2);
        assertEquals(data2.getVersion(), deployedProjectVersion);
        projectVersionCacheDB.closeDb();
    }

    private Repository createRepository(File local) {
        return new GitRepositoryFactory().create(s -> {
            switch (s) {
                case "local-repository-path":
                    return local.getAbsolutePath();
                case "comment-template":
                    return "WebStudio: {commit-type}. {user-message}";
            }
            return null;
        });
    }

    private FileData createFileData(String path, String text) {
        FileData fileData = new FileData();
        fileData.setName(path);
        fileData.setSize(text.length());
        fileData.setComment(text + "-comment");
        fileData.setAuthor("DEFAULT");
        return fileData;
    }
}
