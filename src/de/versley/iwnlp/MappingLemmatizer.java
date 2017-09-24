package de.versley.iwnlp;

import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MappingLemmatizer {
    Map<String, Root.WordForm> _mapping;
    private static IBindingFactory bfact;

    static {
        try {
            bfact = BindingDirectory.getFactory(Root.class);
        } catch (JiBXException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void  addLemma(String surface, String pos, String lemma) {
        Root.WordForm wf;
        if (_mapping.containsKey(surface)) {
            wf = _mapping.get(surface);
        } else {
            wf = new Root.WordForm();
            wf.setForm(surface);
            _mapping.put(surface, wf);
        }
        Root.WordForm.LemmatizerItem item = new Root.WordForm.LemmatizerItem();
        item.setForm(surface);
        item.setLemma(lemma);
        item.setPOS(pos);
        wf.getLemmaList().add(item);
    }

    public MappingLemmatizer(Root mappingOrig) {
        Map<String, String> tagMap = new HashMap<>();
        _mapping = new HashMap<String, Root.WordForm>();
        for (Root.WordForm form: mappingOrig.getWordFormList()) {
            _mapping.put(form.getForm(), form);
            // convert tags to uppercase
            for (Root.WordForm.LemmatizerItem item: form.getLemmaList()) {
                String unnormTag = item.getPOS();
                if (tagMap.containsKey(unnormTag)) {
                    item.setPOS(tagMap.get(unnormTag));
                } else {
                    String normTag;
                    if ("AdjectivalDeclension".equals(unnormTag)) {
                        normTag = "NOUN";
                    } else {
                        normTag = unnormTag.toUpperCase();
                        if (normTag.length() > 4) normTag = normTag.substring(0, 4);
                        if ("ADJE".equals(normTag)) {
                            normTag = "ADJ";
                        }
                    }
                    tagMap.put(unnormTag, normTag);
                    item.setPOS(normTag);
                }
                // use the main form string in all entries
                if (item.getForm().equals(form.getForm())) {
                    item.setForm(form.getForm());
                }
            }
        }
        addLemma("der", "DET", "die");
        addLemma("das", "DET", "die");
        addLemma("die", "DET", "die");
        addLemma("ein", "DET", "ein");
        addLemma("eine", "DET", "ein");
        addLemma("einem", "DET", "ein");
        addLemma("einer", "DET", "ein");
        addLemma("eines", "DET", "ein");
    }

    public String lemmatizeSingle(String form, String tag, boolean addMarker) {
        Root.WordForm wf = _mapping.get(form.toLowerCase());
        String found = null;
        if (wf != null) {
            int match_score = -1;
            for (Root.WordForm.LemmatizerItem item : wf.getLemmaList()) {
                if (tag != null && tag.equals(item.getPOS())) {
                    found = item.getLemma();
                    match_score = 1;
                } else if (match_score < 1 && !"X".equals(item.getPOS())) {
                    found = addMarker?"?"+item.getLemma():item.getLemma();
                    match_score = 0;
                }
            }
        }
        if (found==null && addMarker) {
            found = addMarker?"*"+form:form;
        }
        return found;
    }

    public List<String> lemmatize(List<String> forms, List<String> tags) {
        List<String> result = new ArrayList<>();
        for (int i=0; i<forms.size(); i++) {
            String form = forms.get(i);
            String tag = tags.get(i);
                result.add(lemmatizeSingle(form, tag, true));
        }
        return result;
    }

    public static MappingLemmatizer load(InputStream inputStream) {
        try {
            IUnmarshallingContext uctx = bfact.createUnmarshallingContext();
            Root doc = (Root) uctx.unmarshalDocument(inputStream, null);
            return new MappingLemmatizer(doc);
        } catch (JiBXException ex) {
            throw new RuntimeException("Cannot load lemma table", ex);
        }
    }

    public static void main(String[] args) {
        try {
            MappingLemmatizer lemmatizer = load(
                    new FileInputStream(args[0]));
            String line;
            BufferedReader rd = new BufferedReader(new InputStreamReader(System.in, Charset.forName("UTF-8")));
            while ((line = rd.readLine()) != null) {
                String[] fields = line.split("\\s+");
                if (fields.length >= 2) {
                    String lemma = lemmatizer.lemmatizeSingle(fields[0], fields[1], true);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
