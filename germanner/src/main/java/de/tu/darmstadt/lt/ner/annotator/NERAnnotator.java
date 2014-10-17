package de.tu.darmstadt.lt.ner.annotator;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.classifier.CleartkSequenceAnnotator;
import org.cleartk.classifier.Instance;
import org.cleartk.classifier.feature.extractor.simple.SimpleFeatureExtractor;
import org.uimafit.descriptor.ConfigurationParameter;
import org.uimafit.util.JCasUtil;

import com.thoughtworks.xstream.XStream;

import de.tu.darmstadt.lt.ner.preprocessing.XStreamFactory;
import de.tu.darmstadt.lt.ner.types.GoldNamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class NERAnnotator
    extends CleartkSequenceAnnotator<String>
{
    public static final String PARAM_FEATURE_EXTRACTION_FILE = "FeatureExtractionFile";

    /**
     * if a feature extraction/context extractor filename is given the xml file is parsed and the
     * features are used, otherwise it will not be used
     */
    @ConfigurationParameter(name = PARAM_FEATURE_EXTRACTION_FILE, mandatory = false)
    private String featureExtractionFile = null;

    private List<SimpleFeatureExtractor> featureExtractors;

    @SuppressWarnings("unchecked")
    @Override
    public void initialize(UimaContext context)
        throws ResourceInitializationException
    {
        super.initialize(context);

        // load the settings from a file
        // initialize the XStream if a xml file is given:
        XStream xstream = XStreamFactory.createXStream();
        featureExtractors = (List<SimpleFeatureExtractor>) xstream.fromXML(new File(
                featureExtractionFile));
    }

    @Override
    public void process(JCas jCas)
        throws AnalysisEngineProcessException
    {
        Map<Sentence, Collection<Token>> sentencesTokens = JCasUtil.indexCovered(jCas,
                Sentence.class, Token.class);

        int k = 1;
        for (Sentence sentence : sentencesTokens.keySet()) {
            List<Instance<String>> instances = new ArrayList<Instance<String>>();
            for (Token token : sentencesTokens.get(sentence)) {
                k++;
                Instance<String> instance = new Instance<String>();
                for (SimpleFeatureExtractor extractor : this.featureExtractors) {
                    instance.addAll(extractor.extract(jCas, token));
                }

                if (this.isTraining()) {
                    GoldNamedEntity goldNE = JCasUtil.selectCovered(jCas, GoldNamedEntity.class,
                            token).get(0);
                    instance.setOutcome(goldNE.getNamedEntityType());
                }

                // add the instance to the list !!!
                instances.add(instance);
            }

            // differentiate between training and classifying
            if (this.isTraining()) {
                this.dataWriter.write(instances);
            }
            else {
                List<String> namedEntities = this.classify(instances);
                int i = 0;
                for (Token token : sentencesTokens.get(sentence)) {
                    NamedEntity namedEntity = new NamedEntity(jCas, token.getBegin(),
                            token.getEnd());
                    namedEntity.setValue(namedEntities.get(i));
                    namedEntity.addToIndexes();

                    i++;
                }
            }

            if (k % 200 == 0) {
                System.out.println(k);
            }
        }
    }
}