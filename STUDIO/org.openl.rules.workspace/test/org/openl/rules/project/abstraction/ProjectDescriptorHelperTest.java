package org.openl.rules.project.abstraction;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openl.rules.common.ProjectDescriptor;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.common.impl.ProjectDescriptorImpl;
import org.openl.util.IOUtils;

public class ProjectDescriptorHelperTest {
    private static final String XML = "<descriptors>\n" + "  <descriptor>\n" + "    <repositoryId>design</repositoryId>\n" + "    <projectName>project1</projectName>\n" + "    <projectVersion>3.5.11</projectVersion>\n" + "  </descriptor>\n" + "  <descriptor>\n" + "    <repositoryId>design2</repositoryId>\n" + "    <projectName>project2</projectName>\n" + "    <projectVersion>17</projectVersion>\n" + "  </descriptor>\n" + "  <descriptor>\n" + "    <projectName>project0</projectName>\n" + "    <projectVersion>0</projectVersion>\n" + "  </descriptor>\n" + "</descriptors>";

    @Test
    public void serialize() throws IOException {
        List<ProjectDescriptor> descriptors = makeDescriptors();
        InputStream stream = ProjectDescriptorHelper.serialize(descriptors);
        String xml = IOUtils.toStringAndClose(stream);
        Assert.assertEquals(XML, xml);

    }

    @Test
    public void serializeEmpty() throws IOException {
        InputStream stream = ProjectDescriptorHelper.serialize(null);
        String xml = IOUtils.toStringAndClose(stream);
        Assert.assertEquals("<descriptors/>", xml);

        stream = ProjectDescriptorHelper.serialize(Collections.emptyList());
        xml = IOUtils.toStringAndClose(stream);
        Assert.assertEquals("<descriptors/>", xml);

    }

    private static List<ProjectDescriptor> makeDescriptors() {
        ProjectDescriptor prj1 = new ProjectDescriptorImpl("design",
            "project1",
            null,
            null,
            new CommonVersionImpl(3, 5, 11));
        ProjectDescriptor prj2 = new ProjectDescriptorImpl("design2",
            "project2",
            null,
            null,
            new CommonVersionImpl(17));
        ProjectDescriptor prj3 = new ProjectDescriptorImpl(null, "project0", null, null, new CommonVersionImpl(0));
        return new ArrayList<>(Arrays.asList(prj1, prj2, prj3));
    }

    @Test
    public void deserialize() {
        InputStream stream = IOUtils.toInputStream(XML);
        List<ProjectDescriptor> result = ProjectDescriptorHelper.deserialize(stream);
        List<ProjectDescriptor> expected = makeDescriptors();
        Assert.assertEquals(expected.size(), result.size());
        Assert.assertEquals(expected.get(0).getProjectName(), result.get(0).getProjectName());
        Assert.assertEquals(expected.get(0).getProjectVersion(), result.get(0).getProjectVersion());
        Assert.assertEquals(expected.get(1).getProjectName(), result.get(1).getProjectName());
        Assert.assertEquals(expected.get(1).getProjectVersion(), result.get(1).getProjectVersion());
        Assert.assertEquals(expected.get(2).getProjectName(), result.get(2).getProjectName());
        Assert.assertEquals(expected.get(2).getProjectVersion(), result.get(2).getProjectVersion());
    }

    @Test
    public void deserializeEmpty() {
        InputStream stream = IOUtils.toInputStream("");
        List<ProjectDescriptor> result = ProjectDescriptorHelper.deserialize(stream);
        Assert.assertNull(result);
    }

    @Test
    public void deserializeBlank() {
        InputStream stream = IOUtils.toInputStream("<descriptors/>");
        List<ProjectDescriptor> result = ProjectDescriptorHelper.deserialize(stream);
        Assert.assertEquals(0, result.size());
    }
}
