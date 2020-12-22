package org.openl.rules.project.abstraction;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.openl.rules.common.ProjectDescriptor;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.common.impl.ProjectDescriptorImpl;
import org.openl.util.CollectionUtils;
import org.openl.util.IOUtils;

/**
 * Created by ymolchan on 10/6/2014.
 */
public class ProjectDescriptorHelper {

    public static InputStream serialize(List<ProjectDescriptor> descriptors) {
        if (CollectionUtils.isEmpty(descriptors)) {
            return IOUtils.toInputStream("<descriptors/>");
        }
        StringBuilder builder = new StringBuilder("<descriptors>\n");

        for (ProjectDescriptor descriptor : descriptors) {
            builder.append("  <descriptor>\n");
            if (descriptor.getRepositoryId() != null) {
                builder.append("    <repositoryId>").append(descriptor.getRepositoryId()).append("</repositoryId>\n");
            }
            if (descriptor.getPath() != null) {
                builder.append("    <path>").append(descriptor.getPath()).append("</path>\n");
            }
            builder.append("    <projectName>").append(descriptor.getProjectName()).append("</projectName>\n");
            if (descriptor.getBranch() != null) {
                builder.append("    <branch>").append(descriptor.getBranch()).append("</branch>\n");
            }
            builder.append("    <projectVersion>")
                .append(descriptor.getProjectVersion().getVersionName())
                .append("</projectVersion>\n");
            builder.append("  </descriptor>\n");
        }
        builder.append("</descriptors>");
        return IOUtils.toInputStream(builder.toString());
    }

    @SuppressWarnings({ "rawtypes" })
    public static List<ProjectDescriptor> deserialize(InputStream source) {
        List<ProjectDescriptor> result = null;
        XMLInputFactory factory = XMLInputFactory.newInstance();
        // disable external entities
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        try {
            if (source.available() == 0) {
                return result;
            }
            XMLStreamReader streamReader = factory.createXMLStreamReader(source);

            while (streamReader.hasNext()) {
                streamReader.next();

                switch (streamReader.getEventType()) {
                    case XMLStreamConstants.START_ELEMENT:
                        if (!"descriptor".equals(streamReader.getLocalName())) {
                            result = parseListOfDescripors(streamReader);
                        } else {
                            throw new IllegalStateException(
                                String.format("An inappropriate element <%s>", streamReader.getLocalName()));
                        }
                        break;
                    case XMLStreamConstants.END_ELEMENT:
                        throw new IllegalStateException(
                            String.format("An inappropriate closing element </%s>", streamReader.getLocalName()));
                    case XMLStreamConstants.END_DOCUMENT:
                        return result;

                }
            }
        } catch (XMLStreamException | IOException e) {
            throw new IllegalStateException(e);
        }
        throw new IllegalStateException("Unexpected end of the document");
    }

    private static List<ProjectDescriptor> parseListOfDescripors(
            XMLStreamReader streamReader) throws XMLStreamException {
        ArrayList<ProjectDescriptor> result = new ArrayList<>();
        while (streamReader.hasNext()) {
            streamReader.next();
            switch (streamReader.getEventType()) {
                case XMLStreamConstants.START_ELEMENT:
                    if ("descriptor".equals(streamReader.getLocalName())) {
                        result.add(parseDescripor(streamReader));
                    } else {
                        throw new IllegalStateException(
                            String.format("An inappropriate element <%s>", streamReader.getLocalName()));
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if (!"descriptors".equals(streamReader.getLocalName())) {
                        throw new IllegalStateException(
                            String.format("An inappropriate closing element </%s>", streamReader.getLocalName()));
                    }
                    return result;
            }
        }
        throw new IllegalStateException("Unexpected end of the document");
    }

    private static ProjectDescriptor parseDescripor(XMLStreamReader streamReader) throws XMLStreamException {
        String repositoryId = null;
        String projectName = null;
        String path = null;
        String branch = null;
        String projectVersion = null;
        while (streamReader.hasNext()) {
            streamReader.next();

            switch (streamReader.getEventType()) {
                case XMLStreamConstants.START_ELEMENT:
                    String localName = streamReader.getLocalName();
                    if ("projectName".equals(localName)) {
                        projectName = parseElementAsString("projectName", streamReader);
                    } else if ("projectVersion".equals(localName)) {
                        projectVersion = parseElementAsString("projectVersion", streamReader);
                    } else if ("repositoryId".equals(localName)) {
                        repositoryId = parseElementAsString("repositoryId", streamReader);
                    } else if ("path".equals(localName)) {
                        path = parseElementAsString("path", streamReader);
                    } else if ("branch".equals(localName)) {
                        branch = parseElementAsString("branch", streamReader);
                    } else {
                        throw new IllegalStateException(String.format("An inappropriate element <%s>", localName));
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if (!"descriptor".equals(streamReader.getLocalName())) {
                        throw new IllegalStateException(
                            String.format("An inappropriate closing element </%s>", streamReader.getLocalName()));
                    }
                    CommonVersionImpl commonVersion = new CommonVersionImpl(projectVersion);
                    return new ProjectDescriptorImpl(repositoryId, projectName, path, branch, commonVersion);
            }
        }
        throw new IllegalStateException("Unexpected end of the document");
    }

    private static String parseElementAsString(String element, XMLStreamReader streamReader) throws XMLStreamException {
        String result = null;
        while (streamReader.hasNext()) {
            streamReader.next();

            switch (streamReader.getEventType()) {
                case XMLStreamConstants.CHARACTERS:
                    result = streamReader.getText();
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if (!element.equals(streamReader.getLocalName())) {
                        throw new IllegalStateException(
                            String.format("An inappropriate closing element </%s>", streamReader.getLocalName()));
                    }
                    return result;
            }
        }
        throw new IllegalStateException(String.format("<%s> element has not found", element));
    }
}
