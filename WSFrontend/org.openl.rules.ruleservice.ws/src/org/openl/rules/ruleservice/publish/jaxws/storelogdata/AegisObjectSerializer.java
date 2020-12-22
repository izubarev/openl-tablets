package org.openl.rules.ruleservice.publish.jaxws.storelogdata;

import java.io.ByteArrayOutputStream;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.aegis.AegisWriter;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.aegis.type.AegisType;
import org.openl.rules.ruleservice.storelogdata.ObjectSerializer;
import org.openl.rules.ruleservice.storelogdata.ProcessingException;

public class AegisObjectSerializer implements ObjectSerializer {

    private final AegisDatabinding aegisDatabinding;

    public AegisObjectSerializer(AegisDatabinding aegisDatabinding) {
        this.aegisDatabinding = aegisDatabinding;
    }

    @Override
    public String writeValueAsString(Object obj) throws ProcessingException {
        try {
            return marshal(obj);
        } catch (Exception e) {
            throw new ProcessingException(e);
        }
    }

    private String marshal(Object obj) throws Exception {
        AegisContext context = aegisDatabinding.getAegisContext();
        AegisWriter<XMLStreamWriter> writer = context.createXMLStreamWriter();
        AegisType aegisType = context.getTypeMapping().getType(obj.getClass());

        @SuppressWarnings("squid:S2095") // no need to close ByteArrayOutputStream because of it does nothing
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        XMLStreamWriter xmlWriter = null;
        try {
            xmlWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream);

            writer.write(obj,
                new QName("http://logging.ws.ruleservice.rules.openl.org", ""),
                false,
                xmlWriter,
                aegisType);

            return outputStream.toString();
        } finally {
            if (xmlWriter != null) {
                xmlWriter.close();
            }
        }
    }
}
