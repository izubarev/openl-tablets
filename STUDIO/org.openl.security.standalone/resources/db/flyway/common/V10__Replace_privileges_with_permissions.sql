CREATE TABLE ACL_PERMISSION_MAPPING (
    MASK ${bigint} not null,
    AUTHORITY ${varchar}(50) not null,
    PRIMARY KEY (AUTHORITY, MASK)
);

INSERT INTO ACL_PERMISSION_MAPPING (MASK, AUTHORITY) VALUES (16777217, 'VIEW_PROJECTS');        /*VIEW*/
INSERT INTO ACL_PERMISSION_MAPPING (MASK, AUTHORITY) VALUES (16777217, 'EDIT_PROJECTS');        /*VIEW*/
INSERT INTO ACL_PERMISSION_MAPPING (MASK, AUTHORITY) VALUES (33554448, 'CREATE_PROJECTS');      /*CREATE*/
INSERT INTO ACL_PERMISSION_MAPPING (MASK, AUTHORITY) VALUES (50331664, 'EDIT_PROJECTS');        /*APPEND*/
INSERT INTO ACL_PERMISSION_MAPPING (MASK, AUTHORITY) VALUES (67108892, 'EDIT_PROJECTS');        /*EDIT*/
INSERT INTO ACL_PERMISSION_MAPPING (MASK, AUTHORITY) VALUES (83886084, 'DELETE_PROJECTS');      /*ARCHIVE*/
INSERT INTO ACL_PERMISSION_MAPPING (MASK, AUTHORITY) VALUES (100663304, 'ERASE_PROJECTS');      /*DELETE*/
INSERT INTO ACL_PERMISSION_MAPPING (MASK, AUTHORITY) VALUES (201326592, 'RUN');                 /* RUN */
INSERT INTO ACL_PERMISSION_MAPPING (MASK, AUTHORITY) VALUES (201326592, 'TRACE');               /* RUN */
INSERT INTO ACL_PERMISSION_MAPPING (MASK, AUTHORITY) VALUES (218103808, 'BENCHMARK');           /* BENCHMARK */

INSERT INTO ACL_PERMISSION_MAPPING (MASK, AUTHORITY) VALUES (184549376, 'DEPLOY_PROJECTS');     /* DEPLOY */
INSERT INTO ACL_PERMISSION_MAPPING (MASK, AUTHORITY) VALUES (16777217, 'VIEW_DEPLOYMENTS');     /* VIEW */
INSERT INTO ACL_PERMISSION_MAPPING (MASK, AUTHORITY) VALUES (16777217, 'EDIT_DEPLOYMENT');      /* VIEW */
INSERT INTO ACL_PERMISSION_MAPPING (MASK, AUTHORITY) VALUES (67108892, 'EDIT_DEPLOYMENT');      /* EDIT */
INSERT INTO ACL_PERMISSION_MAPPING (MASK, AUTHORITY) VALUES (50331664, 'EDIT_DEPLOYMENT');      /* APPEND */
INSERT INTO ACL_PERMISSION_MAPPING (MASK, AUTHORITY) VALUES (33554448, 'CREATE_DEPLOYMENT');    /* CREATE */
INSERT INTO ACL_PERMISSION_MAPPING (MASK, AUTHORITY) VALUES (83886084, 'DELETE_DEPLOYMENT');    /* ARCHIVE */
INSERT INTO ACL_PERMISSION_MAPPING (MASK, AUTHORITY) VALUES (100663304, 'ERASE_DEPLOYMENT');    /* DELETE */

INSERT INTO OpenL_Group_Authorities (groupID, authority) SELECT id, 'VIEW_DEPLOYMENTS' FROM OpenL_Groups WHERE groupName = 'Viewers';

INSERT INTO ACL_SID (PRINCIPAL, SID) VALUES (false, 'Administrators');
INSERT INTO ACL_SID (PRINCIPAL, SID) VALUES (true, 'admin');
INSERT INTO ACL_SID (PRINCIPAL, SID) SELECT DISTINCT false, t2.GROUPNAME FROM OpenL_Group_Authorities t1 INNER JOIN OpenL_Groups t2 ON t1.GROUPID = t2.ID WHERE AUTHORITY in (SELECT apm.AUTHORITY FROM ACL_PERMISSION_MAPPING AS apm);

INSERT INTO ACL_CLASS (CLASS, CLASS_ID_TYPE) VALUES ('org.openl.security.acl.repository.ProjectArtifact', 'java.lang.String');
INSERT INTO ACL_CLASS (CLASS, CLASS_ID_TYPE) VALUES ('org.openl.security.acl.repository.Root', 'java.lang.String');

INSERT INTO ACL_OBJECT_IDENTITY (OBJECT_ID_CLASS, OBJECT_ID_IDENTITY, PARENT_OBJECT, OWNER_SID, ENTRIES_INHERITING)
SELECT a.ID, '1', null, c.ID, true
FROM ACL_CLASS a,
     ACL_SID c
WHERE a.CLASS = 'org.openl.security.acl.repository.Root'
  AND a.CLASS_ID_TYPE = 'java.lang.String'
  AND c.PRINCIPAL = false
  AND c.SID = 'Administrators';

INSERT INTO ACL_OBJECT_IDENTITY (OBJECT_ID_CLASS, OBJECT_ID_IDENTITY, PARENT_OBJECT, OWNER_SID, ENTRIES_INHERITING)
SELECT a.ID, 'design', b.ID, c.ID, true
FROM ACL_CLASS a,
     ACL_OBJECT_IDENTITY b,
     ACL_SID c
WHERE a.CLASS = 'org.openl.security.acl.repository.ProjectArtifact'
  AND a.CLASS_ID_TYPE = 'java.lang.String'
  AND b.OBJECT_ID_IDENTITY = '1'
  AND c.PRINCIPAL = false
  AND c.SID = 'Administrators';

CREATE SEQUENCE ACE_ENTRY_ROW_NUM_1;
ALTER SEQUENCE ACE_ENTRY_ROW_NUM_1 RESTART WITH 1;

INSERT INTO ACL_ENTRY (ACL_OBJECT_IDENTITY, SID, MASK, GRANTING, AUDIT_SUCCESS, AUDIT_FAILURE, ACE_ORDER)
    SELECT e.bid, e.aid, e.mask, true, false, false, (SELECT count(*) FROM ACL_ENTRY d WHERE e.bid = d.ACL_OBJECT_IDENTITY) + nextval('ACE_ENTRY_ROW_NUM_1') - 1
    FROM (
        SELECT DISTINCT b.ID as bid, a.ID as aid, c.MASK as mask
        FROM ACL_SID a,
             ACL_OBJECT_IDENTITY b,
             ACL_PERMISSION_MAPPING c,
             (SELECT DISTINCT t2.GROUPNAME AS GROUPNAME, t1.AUTHORITY AS AUTHORITY
                        FROM OpenL_Group_Authorities t1
                                 INNER JOIN OpenL_Groups t2 ON t1.GROUPID = t2.ID
                        WHERE t1.AUTHORITY IN (SELECT apm.AUTHORITY FROM ACL_PERMISSION_MAPPING AS apm)) AS d
        WHERE b.OBJECT_ID_IDENTITY = '1'
          AND a.PRINCIPAL = false
          AND c.AUTHORITY = d.AUTHORITY
          AND a.SID = d.GROUPNAME
        ) as e;

DELETE FROM OpenL_Group_Authorities WHERE AUTHORITY IN (SELECT apm.AUTHORITY FROM ACL_PERMISSION_MAPPING AS apm WHERE apm.AUTHORITY <> 'VIEW_PROJECTS');

DELETE FROM OpenL_Group_Authorities WHERE AUTHORITY IN ('CREATE_TABLES', 'EDIT_TABLES', 'REMOVE_TABLES');

DROP SEQUENCE ACE_ENTRY_ROW_NUM_1;
DROP TABLE ACL_PERMISSION_MAPPING;