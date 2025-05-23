// Copyright Vespa.ai. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.model.ml;

import ai.vespa.rankingexpression.importer.configmodelview.MlModelImporter;
import ai.vespa.rankingexpression.importer.lightgbm.LightGBMImporter;
import ai.vespa.rankingexpression.importer.onnx.OnnxImporter;
import ai.vespa.rankingexpression.importer.vespa.VespaImporter;
import ai.vespa.rankingexpression.importer.xgboost.XGBoostImporter;
import com.yahoo.config.FileReference;
import com.yahoo.config.application.api.ApplicationFile;
import com.yahoo.config.model.ApplicationPackageTester;
import com.yahoo.config.model.api.ApplicationClusterEndpoint;
import com.yahoo.config.model.api.ContainerEndpoint;
import com.yahoo.config.model.api.OnnxModelCost;
import com.yahoo.config.model.api.OnnxModelCost.Calculator;
import com.yahoo.config.model.api.OnnxModelCost.ModelInfo;
import com.yahoo.config.model.api.OnnxModelOptions;
import com.yahoo.config.model.deploy.DeployState;
import com.yahoo.io.GrowableByteBuffer;
import com.yahoo.io.IOUtils;
import com.yahoo.path.Path;
import com.yahoo.tensor.Tensor;
import com.yahoo.tensor.serialization.TypedBinaryFormat;
import com.yahoo.vespa.model.VespaModel;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Helper for testing of imported models.
 * More duplicated functionality across tests on imported models should be moved here
 *
 * @author bratseth
 */
public class ImportedModelTester {

    private final List<MlModelImporter> importers = List.of(new OnnxImporter(),
                                                            new LightGBMImporter(),
                                                            new XGBoostImporter(),
                                                            new VespaImporter());

    private final String modelName;
    private final Path applicationDir;
    private final DeployState deployState;
    public final Calculator calculator = new MockCalculator();

    public ImportedModelTester(String modelName, Path applicationDir) {
        this(modelName, applicationDir, new DeployState.Builder());
    }

    public ImportedModelTester(String modelName, Path applicationDir, DeployState.Builder deployStateBuilder) {
        this.modelName = modelName;
        this.applicationDir = applicationDir;
        deployState = deployStateBuilder.applicationPackage(ApplicationPackageTester.create(applicationDir.toString()).app())
                                        .endpoints(Set.of(new ContainerEndpoint("container", ApplicationClusterEndpoint.Scope.zone, List.of("default.example.com"))))
                                        .modelImporters(importers)
                                        .onnxModelCost((pkg, app, cluster) -> calculator)
                                        .build();
    }

    public VespaModel createVespaModel() {
        try {
            return new VespaModel(deployState);
        }
        catch (SAXException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Verifies that the constant with the given name exists, and - only if an expected size is given -
     * that the content of the constant is available and has the expected size.
     */
    public void assertLargeConstant(String constantName, VespaModel model, Optional<Long> expectedSize) {
        try {
            Path constantApplicationPackagePath = Path.fromString("models.generated/" + modelName + "/constants").append(constantName + ".tbf");
            var constant = model.rankProfileList().constants().asMap().get(constantName);
            assertNotNull(constant);
            assertEquals(constantName, constant.getName());
            assertTrue(constant.getFileName().endsWith(constantApplicationPackagePath.toString()));

            assertTrue(model.fileReferences().contains(new FileReference(constant.getFileName())));

            if (expectedSize.isPresent()) {
                Path constantPath = applicationDir.append(constantApplicationPackagePath);
                assertTrue(constantPath.toFile().exists(),
                           "Constant file '" + constantPath + "' has been written");
                Tensor deserializedConstant = TypedBinaryFormat.decode(Optional.empty(),
                                                                       GrowableByteBuffer.wrap(IOUtils.readFileBytes(constantPath.toFile())));
                assertEquals(expectedSize.get().longValue(), deserializedConstant.size());
            }
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static class MockCalculator implements OnnxModelCost.Calculator {
        private final Map<String, ModelInfo> models = new HashMap<>();
        @Override public long aggregatedModelCostInBytes() { return models.size(); }
        @Override public void registerModel(ApplicationFile path, OnnxModelOptions onnxModelOptions) {
            models.put(path.toString(), new ModelInfo(path.toString(), 1, 1, onnxModelOptions));
        }
        @Override public void registerModel(URI uri, OnnxModelOptions onnxModelOptions) {
            models.put(uri.toString(), new ModelInfo(uri.toString(), 1, 1, onnxModelOptions));
        }
        @Override public Map<String, ModelInfo> models() { return Map.copyOf(models); }
        @Override public void setRestartOnDeploy() { }
        @Override public boolean restartOnDeploy() { return false; }
        @Override public void store() { }
    }

}
