CREATE TABLE ACL_PERMISSION_MAPPING (
    MASK ${bigint} not null,
    AUTHORITY ${varchar}(50) not null,
    CLASS ${varchar}(255) not null,
    OBJECT_ID_IDENTITY ${varchar}(255) not null,
    PRIMARY KEY (AUTHORITY, MASK)
);

CREATE TABLE ACL_REPO_ROOT (
    OBJECT_ID_IDENTITY ${varchar}(255) not null,
    PRIMARY KEY (OBJECT_ID_IDENTITY)
);

INSERT INTO ACL_REPO_ROOT (OBJECT_ID_IDENTITY) VALUES ('1'); /* Design */
INSERT INTO ACL_REPO_ROOT (OBJECT_ID_IDENTITY) VALUES ('2'); /* Deploy Configuration */
INSERT INTO ACL_REPO_ROOT (OBJECT_ID_IDENTITY) VALUES ('3'); /* Production */

INSERT INTO ACL_PERMISSION_MAPPING (CLASS, OBJECT_ID_IDENTITY, MASK, AUTHORITY) VALUES ('org.openl.security.acl.repository.Root', '1', 16777217, 'VIEW_PROJECTS');        /*VIEW*/
INSERT INTO ACL_PERMISSION_MAPPING (CLASS, OBJECT_ID_IDENTITY, MASK, AUTHORITY) VALUES ('org.openl.security.acl.repository.Root', '1', 16777217, 'EDIT_PROJECTS');        /*VIEW*/
INSERT INTO ACL_PERMISSION_MAPPING (CLASS, OBJECT_ID_IDENTITY, MASK, AUTHORITY) VALUES ('org.openl.security.acl.repository.Root', '1', 33554448, 'CREATE_PROJECTS');      /*CREATE*/
INSERT INTO ACL_PERMISSION_MAPPING (CLASS, OBJECT_ID_IDENTITY, MASK, AUTHORITY) VALUES ('org.openl.security.acl.repository.Root', '1', 50331664, 'EDIT_PROJECTS');        /*APPEND*/
INSERT INTO ACL_PERMISSION_MAPPING (CLASS, OBJECT_ID_IDENTITY, MASK, AUTHORITY) VALUES ('org.openl.security.acl.repository.Root', '1', 67108892, 'EDIT_PROJECTS');        /*EDIT*/
INSERT INTO ACL_PERMISSION_MAPPING (CLASS, OBJECT_ID_IDENTITY, MASK, AUTHORITY) VALUES ('org.openl.security.acl.repository.Root', '1', 83886084, 'DELETE_PROJECTS');      /*ARCHIVE*/
INSERT INTO ACL_PERMISSION_MAPPING (CLASS, OBJECT_ID_IDENTITY, MASK, AUTHORITY) VALUES ('org.openl.security.acl.repository.Root', '1', 100663304, 'ERASE_PROJECTS');      /*DELETE*/
INSERT INTO ACL_PERMISSION_MAPPING (CLASS, OBJECT_ID_IDENTITY, MASK, AUTHORITY) VALUES ('org.openl.security.acl.repository.Root', '1', 201326592, 'RUN');                 /* RUN */
INSERT INTO ACL_PERMISSION_MAPPING (CLASS, OBJECT_ID_IDENTITY, MASK, AUTHORITY) VALUES ('org.openl.security.acl.repository.Root', '1', 201326592, 'TRACE');               /* RUN */
INSERT INTO ACL_PERMISSION_MAPPING (CLASS, OBJECT_ID_IDENTITY, MASK, AUTHORITY) VALUES ('org.openl.security.acl.repository.Root', '1', 218103808, 'BENCHMARK');           /* BENCHMARK */
INSERT INTO ACL_PERMISSION_MAPPING (CLASS, OBJECT_ID_IDENTITY, MASK, AUTHORITY) VALUES ('org.openl.security.acl.repository.Root', '1', 184549376, 'DEPLOY_PROJECTS');     /* DEPLOY */

INSERT INTO ACL_PERMISSION_MAPPING (CLASS, OBJECT_ID_IDENTITY, MASK, AUTHORITY) VALUES ('org.openl.security.acl.repository.DeploymentProjectArtifact', 'deploy-config', 16777217, 'VIEW_DEPLOYMENTS');     /* VIEW */
INSERT INTO ACL_PERMISSION_MAPPING (CLASS, OBJECT_ID_IDENTITY, MASK, AUTHORITY) VALUES ('org.openl.security.acl.repository.DeploymentProjectArtifact', 'deploy-config', 16777217, 'EDIT_DEPLOYMENT');      /* VIEW */
INSERT INTO ACL_PERMISSION_MAPPING (CLASS, OBJECT_ID_IDENTITY, MASK, AUTHORITY) VALUES ('org.openl.security.acl.repository.DeploymentProjectArtifact', 'deploy-config', 67108892, 'EDIT_DEPLOYMENT');      /* EDIT */
INSERT INTO ACL_PERMISSION_MAPPING (CLASS, OBJECT_ID_IDENTITY, MASK, AUTHORITY) VALUES ('org.openl.security.acl.repository.DeploymentProjectArtifact', 'deploy-config', 50331664, 'EDIT_DEPLOYMENT');      /* APPEND */
INSERT INTO ACL_PERMISSION_MAPPING (CLASS, OBJECT_ID_IDENTITY, MASK, AUTHORITY) VALUES ('org.openl.security.acl.repository.DeploymentProjectArtifact', 'deploy-config', 33554448, 'CREATE_DEPLOYMENT');    /* CREATE */
INSERT INTO ACL_PERMISSION_MAPPING (CLASS, OBJECT_ID_IDENTITY, MASK, AUTHORITY) VALUES ('org.openl.security.acl.repository.DeploymentProjectArtifact', 'deploy-config', 83886084, 'DELETE_DEPLOYMENT');    /* ARCHIVE */
INSERT INTO ACL_PERMISSION_MAPPING (CLASS, OBJECT_ID_IDENTITY, MASK, AUTHORITY) VALUES ('org.openl.security.acl.repository.DeploymentProjectArtifact', 'deploy-config', 100663304, 'ERASE_DEPLOYMENT');    /* DELETE */

INSERT INTO ACL_PERMISSION_MAPPING (CLASS, OBJECT_ID_IDENTITY, MASK, AUTHORITY) VALUES ('org.openl.security.acl.repository.Root', '3', 67108892, 'DEPLOY_PROJECTS');     /* EDIT */

INSERT INTO OpenL_Group_Authorities (groupID, authority) SELECT id, 'VIEW_DEPLOYMENTS' FROM OpenL_Groups WHERE groupName = 'Viewers';

INSERT INTO ACL_SID (PRINCIPAL, SID) VALUES (false, 'Administrators');
INSERT INTO ACL_SID (PRINCIPAL, SID)
    SELECT DISTINCT false, t2.GROUPNAME
    FROM OPENL_GROUP_AUTHORITIES t1 INNER JOIN OPENL_GROUPS t2 ON t1.GROUPID = t2.ID
    WHERE AUTHORITY in (SELECT apm.AUTHORITY FROM ACL_PERMISSION_MAPPING AS apm);

INSERT INTO ACL_CLASS (CLASS, CLASS_ID_TYPE) VALUES ('org.openl.security.acl.repository.Root', 'java.lang.String');
INSERT INTO ACL_CLASS (CLASS, CLASS_ID_TYPE) VALUES ('org.openl.security.acl.repository.ProjectArtifact', 'java.lang.String');
INSERT INTO ACL_CLASS (CLASS, CLASS_ID_TYPE) VALUES ('org.openl.security.acl.repository.DeploymentProjectArtifact', 'java.lang.String');
INSERT INTO ACL_CLASS (CLASS, CLASS_ID_TYPE) VALUES ('org.openl.security.acl.repository.RepositoryObjectIdentity', 'java.lang.String');

INSERT INTO ACL_OBJECT_IDENTITY (OBJECT_ID_CLASS, OBJECT_ID_IDENTITY, PARENT_OBJECT, OWNER_SID, ENTRIES_INHERITING)
SELECT a.ID, d.OBJECT_ID_IDENTITY, null, b.ID, true
FROM ACL_CLASS a,
     ACL_SID b,
     ACL_REPO_ROOT d
WHERE a.CLASS = 'org.openl.security.acl.repository.Root'
  AND b.PRINCIPAL = false
  AND b.SID = 'Administrators';

INSERT INTO ACL_OBJECT_IDENTITY (OBJECT_ID_CLASS, OBJECT_ID_IDENTITY, PARENT_OBJECT, OWNER_SID, ENTRIES_INHERITING)
SELECT a.ID, 'deploy-config', c.id, b.ID, true
FROM ACL_CLASS a,
     ACL_SID b,
     ACL_OBJECT_IDENTITY c,
     ACL_CLASS d
WHERE a.CLASS = 'org.openl.security.acl.repository.DeploymentProjectArtifact'
  AND b.PRINCIPAL = false
  AND b.SID = 'Administrators'
  AND c.OBJECT_ID_IDENTITY = '2'
  AND d.id = c.OBJECT_ID_CLASS
  AND d.CLASS = 'org.openl.security.acl.repository.Root';

CREATE SEQUENCE ACE_ENTRY_ROW_NUM_1;
ALTER SEQUENCE ACE_ENTRY_ROW_NUM_1 RESTART WITH 1;
INSERT INTO ACL_ENTRY (ACL_OBJECT_IDENTITY, SID, MASK, GRANTING, AUDIT_SUCCESS, AUDIT_FAILURE, ACE_ORDER)
    SELECT e.bid, e.aid, e.mask, true, false, false, (SELECT count(*) FROM ACL_ENTRY d WHERE e.bid = d.ACL_OBJECT_IDENTITY) + nextval('ACE_ENTRY_ROW_NUM_1') - 1
    FROM (
        SELECT DISTINCT b.ID as bid, a.ID as aid, c.MASK as mask
        FROM ACL_SID a,
             ACL_OBJECT_IDENTITY b,
             ACL_PERMISSION_MAPPING c,
             ACL_CLASS d,
             OpenL_Group_Authorities t1 INNER JOIN OpenL_Groups t2 ON t1.GROUPID = t2.ID
        WHERE b.OBJECT_ID_IDENTITY = c.OBJECT_ID_IDENTITY
          AND b.OBJECT_ID_CLASS  = d.id
          AND d.CLASS = c.CLASS
          AND a.PRINCIPAL = false
          AND c.AUTHORITY = t1.AUTHORITY
          AND a.SID = t2.GROUPNAME
        ) as e;

DELETE FROM OpenL_Group_Authorities WHERE AUTHORITY IN (SELECT apm.AUTHORITY FROM ACL_PERMISSION_MAPPING AS apm WHERE apm.AUTHORITY <> 'VIEW_PROJECTS');

DELETE FROM OpenL_Group_Authorities WHERE AUTHORITY IN ('CREATE_TABLES', 'EDIT_TABLES', 'REMOVE_TABLES');

DROP SEQUENCE ACE_ENTRY_ROW_NUM_1;
DROP TABLE ACL_REPO_ROOT;
DROP TABLE ACL_PERMISSION_MAPPING;