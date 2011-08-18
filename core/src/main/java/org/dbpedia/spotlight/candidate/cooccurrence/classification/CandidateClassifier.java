package org.dbpedia.spotlight.candidate.cooccurrence.classification;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.candidate.cooccurrence.weka.InstanceBuilder;
import org.dbpedia.spotlight.candidate.cooccurrence.features.data.OccurrenceDataProvider;
import org.dbpedia.spotlight.exceptions.InitializationException;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;
import weka.classifiers.Classifier;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;


/**
 * Classifier for surface form occurrences based on serialized
 * WEKA classifiers.
 *
 * @author Joachim Daiber
 */

public class CandidateClassifier {

	Log LOG = LogFactory.getLog(this.getClass());

	protected InstanceBuilder instanceBuilder;
	protected Instances header;
	protected Classifier classifier;
	protected String modelFile;
	protected OccurrenceDataProvider dataProvider;
	protected final double MIN_CONFIDENCE = 0.5;

	protected boolean verboseMode = false;

	
	/**
	 * Create a new candidate classifier that was serialized in modelFile and that
	 * uses the provided OccurrenceDataProvider and InstanceBuilder.
	 *
	 * @param modelFile serialized model file
	 * @param dataProvider data provider for occurrence data
	 * @param instanceBuilder builder for WEKA instances
	 * @throws InitializationException when something goes wrong on initialization
	 */
	public CandidateClassifier(String modelFile, OccurrenceDataProvider dataProvider, InstanceBuilder instanceBuilder)
			throws InitializationException {
		
		this.modelFile = modelFile;
		this.dataProvider = dataProvider;
		this.instanceBuilder = instanceBuilder;
		initialize();
	}

	
	/**
	 * Load the serialized classifier model and read the header for new instances from it.
	 *
	 * @throws InitializationException the classifier could not be initialized.
	 */
	protected void initialize() throws InitializationException {

		Object o[];
		try {
			o = SerializationHelper.readAll(modelFile);
		} catch (Exception e) {
			throw new InitializationException("Could not deserialize classifier from file " + modelFile);
		}
		classifier = (Classifier) o[0];
		header = (Instances) o[1];
		header.setClassIndex(header.numAttributes() - 1);
		LOG.trace("Successfully deserialized Classifier " + classifier);

	}

	
	/**
	 * Classify a surface form candidate.
	 *
	 * @param surfaceFormOccurrence the surface form occurrence
	 * @return a Classification object containing the proposed classification and a confidence value
	 */
	public CandidateClassification classify(SurfaceFormOccurrence surfaceFormOccurrence) {
		Instance instance = buildInstance(surfaceFormOccurrence);

		try {
			double candidateClassification = classifier.classifyInstance(instance);

			double[] distributionForInstance = classifier.distributionForInstance(instance);
			double confidence = distributionForInstance[(int) candidateClassification];
			CandidateClass candidateClass = candidateClassification == 0 && confidence > MIN_CONFIDENCE ? CandidateClass.valid : CandidateClass.common;

			return new CandidateClassification(confidence, candidateClass);
		} catch (Exception e) {
			LOG.error("Exception while classifing " + e);
			return null;
		}

	}

	
	/**
	 * Builds a suitable WEKA Instance of the surface form occurrence for
	 * the serialized classifier.
	 *
	 * @param surfaceFormOccurrence surface form occurrence
	 * @return WEKA instance
	 */
	protected Instance buildInstance(SurfaceFormOccurrence surfaceFormOccurrence) {
		Instance instance = new DenseInstance(header.numAttributes());
		instance.setDataset(header);
		return instanceBuilder.buildInstance(surfaceFormOccurrence, instance);
	}

	
	/**
	 * In verbose mode, the classifier logs why and how it made its classification decision.
	 * 
	 * @param verboseMode put classifier in verbose mode?
	 */
	public void setVerboseMode(boolean verboseMode) {
		this.verboseMode = verboseMode;
	}
}
