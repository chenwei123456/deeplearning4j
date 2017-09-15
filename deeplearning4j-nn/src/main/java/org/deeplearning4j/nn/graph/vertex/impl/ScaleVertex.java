/*-
 *
 *  * Copyright 2016 Skymind,Inc.
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */

package org.deeplearning4j.nn.graph.vertex.impl;

import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.api.MaskState;
import org.deeplearning4j.nn.api.activations.Activations;
import org.deeplearning4j.nn.api.activations.ActivationsFactory;
import org.deeplearning4j.nn.api.gradients.Gradients;
import org.deeplearning4j.nn.gradient.Gradient;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.graph.vertex.BaseGraphVertex;
import org.deeplearning4j.nn.graph.vertex.VertexIndices;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.primitives.Pair;

/**
 * A ScaleVertex is used to scale the size of activations of a single layer<br>
 * For example, ResNet activations can be scaled in repeating blocks to keep variance
 * under control.
 *
 * @author Justin Long (@crockpotveggies)
 */
public class ScaleVertex extends BaseGraphVertex {

    private double scaleFactor;

    public ScaleVertex(ComputationGraph graph, String name, int vertexIndex, int numInputs, double scaleFactor) {
        super(graph, name, vertexIndex, numInputs);
        this.scaleFactor = scaleFactor;
    }

    @Override
    public Activations activate(boolean training) {
        if (!canDoForward())
            throw new IllegalStateException("Cannot do forward pass: inputs not set (ScaleVertex " + vertexName
                            + " idx " + vertexIndex + ")");

        if (inputs.length > 1)
            throw new IllegalArgumentException(
                            "ScaleVertex (name " + vertexName + " idx " + vertexIndex + ") only supports 1 input.");

        INDArray prod = inputs[0].dup();
        prod.muli(scaleFactor);

        return ActivationsFactory.getInstance().create(prod);
    }

    @Override
    public Gradients backpropGradient(Gradients gradient) {
        if (!canDoBackward())
            throw new IllegalStateException("Cannot do backward pass: errors not set (ScaleVertex " + vertexName
                            + " idx " + vertexIndex + ")");

        INDArray epsilon = gradient.get(0);
        epsilon.muli(scaleFactor);

        return gradient;
    }

    @Override
    public void setBackpropGradientsViewArray(INDArray backpropGradientsViewArray) {
        if (backpropGradientsViewArray != null)
            throw new RuntimeException(
                            "Vertex does not have gradients; gradients view array cannot be set here (ScaleVertex "
                                            + vertexName + " idx " + vertexIndex + ")");
    }

    @Override
    public String toString() {
        return "ScaleVertex(id=" + this.getIndex() + ",name=\"" + this.getName() + "\",scaleFactor="
                        + scaleFactor + ")";
    }

    @Override
    public Pair<INDArray, MaskState> feedForwardMaskArrays(INDArray[] maskArrays, MaskState currentMaskState,
                    int minibatchSize) {
        //No op
        if (maskArrays == null || maskArrays.length == 0) {
            return null;
        }

        return new Pair<>(maskArrays[0], currentMaskState);
    }
}
