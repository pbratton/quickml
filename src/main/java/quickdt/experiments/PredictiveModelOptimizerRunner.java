package quickdt.experiments;

import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickdt.crossValidation.*;
import quickdt.csvReader.CSVToMap;
import quickdt.data.AbstractInstance;
import quickdt.data.Attributes;
import quickdt.data.Instance;
import quickdt.predictiveModelOptimizer.PredictiveModelOptimizer;
import quickdt.predictiveModels.PredictiveModelBuilderBuilder;
import quickdt.predictiveModels.calibratedPredictiveModel.PAVCalibratedPredictiveModelBuilderBuilder;
import quickdt.predictiveModels.calibratedPredictiveModel.UpdatablePAVCalibratedPredictiveModelBuilderBuilder;
import quickdt.predictiveModels.downsamplingPredictiveModel.DownsamplingPredictiveModelBuilderBuilder;
import quickdt.predictiveModels.randomForest.RandomForestBuilderBuilder;
import quickdt.predictiveModels.randomForest.UpdatableRandomForestBuilderBuilder;

import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by chrisreeves on 5/23/14.
 */
public class PredictiveModelOptimizerRunner {

    private static final Logger logger =  LoggerFactory.getLogger(PredictiveModelOptimizerRunner.class);


    public static void main(String[] args) throws IOException {
        List<PredictiveModelBuilderBuilder> builderBuilders = getPredictiveModelBuilderBuilders();
        Iterable<? extends AbstractInstance> trainingData = getTrainingData("data/redshift_training_data.csv");
        List<BidderConfiguration> configurations = Lists.newLinkedList();

        for(PredictiveModelBuilderBuilder builderBuilder : builderBuilders) {
            CrossValidator crossValidator = getCrossValidator();
            PredictiveModelOptimizer predictiveModelOptimizer = new PredictiveModelOptimizer(builderBuilder, trainingData, crossValidator);
            final Map<String, Object> optimalParameters = predictiveModelOptimizer.determineOptimalConfiguration();
            Double loss = predictiveModelOptimizer.getLoss(optimalParameters);
            configurations.add(new BidderConfiguration(builderBuilder, optimalParameters, loss));
        }

        Collections.sort(configurations, new Comparator<BidderConfiguration>() {
            @Override
            public int compare(final BidderConfiguration o1, final BidderConfiguration o2) {
                return o1.loss.compareTo(o2.loss);
            }
        });

        logger.info(configurations.get(0).builderBuilder.toString() + " loss:" + configurations.get(0).loss + " " + configurations.get(0).optimalParameters);
        logger.info(configurations.get(configurations.size() - 1).builderBuilder.toString() + " loss:" + configurations.get(configurations.size()-1).loss + " " + configurations.get(configurations.size()-1).optimalParameters);
    }

    private static CrossValidator getCrossValidator() {
        return new OutOfTimeCrossValidator(new WeightedAUCCrossValLoss(1.0), 0.25, 30, new TrainingDateTimeExtractor());
    }

    private static List<PredictiveModelBuilderBuilder> getPredictiveModelBuilderBuilders() {
        List<PredictiveModelBuilderBuilder> builderBuilders = Lists.newLinkedList();
        builderBuilders.add(new UpdatableRandomForestBuilderBuilder(new RandomForestBuilderBuilder()));
        return builderBuilders;
    }

    private static Iterable<? extends AbstractInstance> getTrainingData(String filename) throws IOException {
        List<Map<String, Serializable>> instanceMaps = CSVToMap.loadRows(filename);
        List<Instance> instances = csvReaderExp.convertRawMapToInstance(instanceMaps);
        logger.info("Read " + instances.size() + " instances");
        return instances;
    }

    static class BidderConfiguration {
        private Map<String, Object> optimalParameters;
        private PredictiveModelBuilderBuilder builderBuilder;
        private Double loss;

        public BidderConfiguration(PredictiveModelBuilderBuilder builderBuilder, Map<String, Object> optimalParameters, Double loss) {
            this.builderBuilder = builderBuilder;
            this.optimalParameters = optimalParameters;
            this.loss = loss;
        }
    }

    private static class TrainingDateTimeExtractor implements DateTimeExtractor {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        @Override
        public DateTime extractDateTime(AbstractInstance instance) {
            Attributes attributes = instance.getAttributes();
            try {
                Date currentTimeMillis = dateFormat.parse((String)attributes.get("created_at"));
                return new DateTime(currentTimeMillis);
            } catch (ParseException e) {
                logger.error("Error parsing date", e);
            }
            return new DateTime();
        }
    }
}
