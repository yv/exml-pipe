package de.versley.exml.importers;

import com.fasterxml.aalto.WFCException;
import com.fasterxml.aalto.evt.EventAllocatorImpl;
import com.fasterxml.aalto.evt.EventReaderImpl;
import com.fasterxml.aalto.in.ByteSourceBootstrapper;
import com.fasterxml.aalto.in.ReaderConfig;
import com.fasterxml.aalto.stax.InputFactoryImpl;
import com.fasterxml.aalto.stax.StreamReaderImpl;
import com.fasterxml.aalto.util.IllegalCharHandler;
import de.versley.exml.pipe.ExmlDocBuilder;
import exml.GenericMarkable;
import exml.MarkableLevel;
import exml.io.DocumentReader;
import exml.io.ReaderStackEntry;
import exml.objects.ObjectSchema;
import exml.tueba.TuebaDocument;
import exml.tueba.TuebaTerminal;
import org.codehaus.stax2.XMLStreamReader2;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Reads the "segments" format, which consists of document structure markables
 * and segments of raw text.
 */
public class SegmentReader extends DocumentReader<TuebaTerminal> {
    ExmlDocBuilder _builder;

    public SegmentReader(XMLEventReader reader, ExmlDocBuilder builder)
        throws XMLStreamException
    {
        super(builder.getDocument(), reader);
        _builder = builder;
    }

    private <M extends GenericMarkable> void push_markable(ObjectSchema<M> schema, StartElement elm)
    {
        M new_m=schema.createMarkable();
        Attribute attr = elm.getAttributeByName(qname_xmlid);
        if (attr != null) {
            new_m.setXMLId(attr.getValue());
        }
        new_m.setStart(_doc.size());
        _doc.nameForObject(new_m);
        setObjectAttributes(new_m,schema,elm);
        _openTags.push(new ReaderStackEntry<M>(schema, new_m));
    }

    public void readBody() throws XMLStreamException {
        if (!_inBody) {
            throw new RuntimeException("should be in body!");
        }
        while (true) {
            XMLEvent ev=_reader.nextTag();
            if (ev==null) {
                System.err.println("nextTag() returned null!");
                return;
            }
            if (ev.isStartElement()) {
                StartElement elm = ev.asStartElement();
                String tagname = elm.getName().getLocalPart();
                if ("raw-text".equals(tagname)) {
                    String text;
                    try {
                        text = _reader.getElementText();
                    } catch(WFCException ex) {
                        ex.printStackTrace();
                        text = "omitted:"+ex.toString();
                    }
                    // System.err.println("raw-text:"+text);
                    _builder.addText(text);
                } else {
                    // System.err.println("open:"+tagname);
                    ObjectSchema schema = _doc.markableSchemaByName(tagname, true);
                    push_markable(schema, elm);
                }
            } else if (ev.isEndElement()) {
                EndElement elm=ev.asEndElement();
                String tagname=elm.getName().getLocalPart();
                // if we're finished with the body, break out of the loop
                if (_openTags.size()==0) {
                    if (!"body".equals(tagname)) {
                        throw new RuntimeException("body closed by "+tagname);
                    }
                    break;
                }
                // do something else
                ReaderStackEntry entry=_openTags.pop();
                // System.err.format("close[%d]:%s %s\n", _openTags.size(), tagname, entry.value);
                entry.value.setEnd(_doc.size());
                MarkableLevel mlvl = _doc.markableLevelByName(entry.schema.getName(), true);
                mlvl.addMarkable(entry.value);
            }
        }
    }

    public static TuebaDocument loadStream(InputStream is, ExmlDocBuilder builder)
        throws FileNotFoundException, XMLStreamException {
        // We use Aalto-XML's internal APIs to make the reader more lenient towards illegal text
        InputFactoryImpl factory = new InputFactoryImpl();
        ReaderConfig cfg = factory.getNonSharedConfig(null, null,
                null, true, false);
        cfg.configureForConvenience();
        cfg.setIllegalCharHandler(new IllegalCharHandler.ReplacingIllegalCharHandler('?'));
        XMLStreamReader2 reader = StreamReaderImpl.construct(
                ByteSourceBootstrapper.construct(cfg, is));
        XMLEventReader xml_reader = new EventReaderImpl(EventAllocatorImpl.getFastInstance(), reader);
        SegmentReader doc_reader =
                new SegmentReader(xml_reader, builder);
        doc_reader.readBody();
        xml_reader.close();
        return builder.getDocument();
    }

    public static TuebaDocument loadDocument(String xmlFile, ExmlDocBuilder builder)
        throws FileNotFoundException, XMLStreamException
    {
        InputStream is = new FileInputStream(xmlFile);
        return loadStream(is, builder);
    }
}
