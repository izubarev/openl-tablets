package org.openl.security.acl.repository;

import java.util.List;
import java.util.Map;

import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;

public interface SimpleRepositoryAclService {

    Map<Sid, List<Permission>> listPermissions(String repositoryId, String path);

    Map<Sid, List<Permission>> listPermissions(String repositoryId, String path, List<Sid> sids);

    Map<Sid, List<Permission>> listRootPermissions();

    Map<Sid, List<Permission>> listRootPermissions(List<Sid> sids);

    void addPermissions(String repositoryId, String path, Map<Sid, List<Permission>> permissions);

    void addPermissions(String repositoryId, String path, List<Permission> permissions, List<Sid> sids);

    void addRootPermissions(Map<Sid, List<Permission>> permissions);

    void addRootPermissions(List<Permission> permissions, List<Sid> sids);

    void move(String repositoryId, String path, String newPath);

    void deleteAcl(String repositoryId, String path);

    void deleteAclRoot();

    void removePermissions(String repositoryId, String path);

    void removePermissions(String repositoryId, String path, List<Sid> sids);

    void removePermissions(String repositoryId, String path, List<Permission> permissions, List<Sid> sids);

    void removePermissions(String repositoryId, String path, Map<Sid, List<Permission>> permissions);

    void removeRootPermissions(List<Permission> permissions, List<Sid> sids);

    void removeRootPermissions(List<Sid> sids);

    void removeRootPermissions();

    boolean isGranted(String repositoryId, String path, List<Permission> permissions);

    boolean createAcl(String repositoryId, String path, List<Permission> permissions, boolean force);

    boolean hasAcl(String repositoryId, String path);

    Sid getOwner(String repositoryId, String path);

    boolean updateOwner(String repositoryId, String path, Sid newOwner);
}
