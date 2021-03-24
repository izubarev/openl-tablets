package org.openl.rules.project.abstraction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class CommentsTest {

    public static final String TEMPLATE = "FOO";

    private Comments comments;
    private Comments comments2;

    @Before
    public void setUp() {
        String dateTimeFormat = "MM/dd/yyyy 'at' hh:mm:ss a";
        String saveProjectTemplate = "Project {username} {{project-name}} is saved. {foo}";
        String createProjectTemplate = "Project {username} {project-name} is created. {foo}";
        String archiveProjectTemplate = "Project {username} {{project-name} is archived. {foo}";
        String restoreProjectTemplate = "Project {username} '{'{project-name} is restored. {foo}";
        String eraseProjectTemplate = "Project {username} {project-name} is erased. {foo}";
        String copiedFromTemplate = "Project {username} {{project-name}} is copied-from. {foo}";
        String restoredFromTemplate = "Project {username} {revision} is restored-from. Author: {author}, date: {datetime}. {foo}";
        String newBranchNameTemplate = "WebStudio/{project-name}/{username}/{current-date} {foo}";
        comments = new Comments(dateTimeFormat,
            saveProjectTemplate,
            createProjectTemplate,
            archiveProjectTemplate,
            restoreProjectTemplate,
            eraseProjectTemplate,
            copiedFromTemplate,
            restoredFromTemplate,
            newBranchNameTemplate);

        comments2 = new Comments(dateTimeFormat, TEMPLATE, TEMPLATE, TEMPLATE, TEMPLATE, TEMPLATE, TEMPLATE, TEMPLATE, TEMPLATE);
    }

    @Test
    public void testSaveProject() {
        String actual = comments.saveProject("myProjectName");
        assertEquals("Project {username} {myProjectName} is saved. {foo}", actual);
    }

    @Test
    public void testSaveProjectWithDollarSign() {
        String actualWithSymbol = comments.saveProject("$$$myProj$ectName$$");
        assertEquals("Project {username} {$$$myProj$ectName$$} is saved. {foo}", actualWithSymbol);
    }

    @Test
    public void testCreateProject() {
        String actual = comments.createProject("myProjectName");
        assertEquals("Project {username} myProjectName is created. {foo}", actual);
    }

    @Test
    public void testCreateProjectWithDollarSign() {
        String actualWithSymbol = comments.createProject("$$$myProj$ectName$$");
        assertEquals("Project {username} $$$myProj$ectName$$ is created. {foo}", actualWithSymbol);
    }

    @Test
    public void testArchiveProject() {
        String actual = comments.archiveProject("myProjectName");
        assertEquals("Project {username} {myProjectName is archived. {foo}", actual);
    }

    @Test
    public void testArchiveProjectWithDollarSign() {
        String actualWithSymbol = comments.archiveProject("$$$myProj$ectName$$");
        assertEquals("Project {username} {$$$myProj$ectName$$ is archived. {foo}", actualWithSymbol);
    }

    @Test
    public void testRestoreProject() {
        String actual = comments.restoreProject("myProjectName");
        assertEquals("Project {username} '{'myProjectName is restored. {foo}", actual);
    }

    @Test
    public void testRestoreProjectWithDollarSign() {
        String actualWithSymbol = comments.restoreProject("$$$myProj$ectName$$");
        assertEquals("Project {username} '{'$$$myProj$ectName$$ is restored. {foo}", actualWithSymbol);
    }

    @Test
    public void testEraseProject() {
        String actual = comments.eraseProject("myProjectName");
        assertEquals("Project {username} myProjectName is erased. {foo}", actual);
    }

    @Test
    public void testEraseProjectWithDollarSign() {
        String actualWithSymbol = comments.eraseProject("$$$myProj$ectName$$");
        assertEquals("Project {username} $$$myProj$ectName$$ is erased. {foo}", actualWithSymbol);
    }

    @Test
    public void testCopiedFrom() {
        String actual = comments.copiedFrom("myProjectName");
        assertEquals("Project {username} {myProjectName} is copied-from. {foo}", actual);
    }

    @Test
    public void testCopiedFromProjectWithSpecialSymbols() {
        String actualWithSymbol = comments.copiedFrom("$$$myProj$ectName$$");
        assertEquals("Project {username} {$$$myProj$ectName$$} is copied-from. {foo}", actualWithSymbol);
    }

    @Test
    public void testParseSourceOfCopy() {
        List<String> commentParts = comments
            .getCommentParts("Project {username} {myProjectName} is copied-from. {foo}");
        assertEquals(3, commentParts.size());
        assertEquals("Project {username} {", commentParts.get(0));
        assertEquals("myProjectName", commentParts.get(1));
        assertEquals("} is copied-from. {foo}", commentParts.get(2));

        List<String> parts2 = comments.getCommentParts(null);
        assertEquals(1, parts2.size());
        assertNull(parts2.get(0));

        List<String> parts3 = comments.getCommentParts("");
        assertEquals(1, parts3.size());
        assertEquals("", parts3.get(0));

        // Not applied to pattern
        List<String> parts4 = comments.getCommentParts("My comment");
        assertEquals(1, parts4.size());
        assertEquals("My comment", parts4.get(0));
    }

    @Test
    public void testRestoredFrom() {
        Date date = new GregorianCalendar(2020, Calendar.JUNE, 22, 21, 2, 42).getTime();
        String actual = comments.restoredFrom("sdsd-s-ds-d-sd-sd", "john", date);
        assertEquals("Project {username} sdsd-s-ds-d-sd-sd is restored-from. Author: john, date: 06/22/2020 at 09:02:42 PM. {foo}", actual);
    }

    @Test
    public void testRestoredFromWithDollarSign() {
        Date date = new GregorianCalendar(2020, Calendar.JUNE, 22, 21, 2, 42).getTime();
        String actualWithSymbol = comments.restoredFrom("$$$12$$3$", "john", date);
        assertEquals("Project {username} $$$12$$3$ is restored-from. Author: john, date: 06/22/2020 at 09:02:42 PM. {foo}", actualWithSymbol);
    }


    @Test
    public void testNewBranch() {
        String actual = comments.newBranch("myProjectName", "myUserName", "myCurrentDate");
        assertEquals("WebStudio/myProjectName/myUserName/myCurrentDate {foo}", actual);
    }

    @Test
    public void testNewBranchWithDollarSign() {
        String actualWithSymbol = comments.newBranch("$$$myProj$ectName$$", "myUserName", "myCurrentDate");
        assertEquals("WebStudio/$$$myProj$ectName$$/myUserName/myCurrentDate {foo}", actualWithSymbol);
    }

    @Test
    public void testSimpleComments() {
        assertEquals(TEMPLATE, comments2.saveProject("foo"));
        assertEquals(TEMPLATE, comments2.createProject("foo"));
        assertEquals(TEMPLATE, comments2.archiveProject("foo"));
        assertEquals(TEMPLATE, comments2.restoreProject("foo"));
        assertEquals(TEMPLATE, comments2.eraseProject("foo"));
        assertEquals(TEMPLATE, comments2.copiedFrom("foo"));
        assertEquals(TEMPLATE, comments2.restoredFrom("foo", "bar", new Date()));
        assertEquals("Project {username} {myProjectName} is copied-from. {foo}",
                comments2.getCommentParts("Project {username} {myProjectName} is copied-from. {foo}").get(0));
    }

}
