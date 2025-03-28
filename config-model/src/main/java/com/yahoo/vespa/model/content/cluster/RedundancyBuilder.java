// Copyright Vespa.ai. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.model.content.cluster;

import com.yahoo.vespa.model.builder.xml.dom.ModelElement;
import com.yahoo.vespa.model.content.IndexedHierarchicDistributionValidator;
import com.yahoo.vespa.model.content.Redundancy;

/**
 * Builds redundancy config for a content cluster.
 *
 * @author bratseth
 */
public class RedundancyBuilder {

    // Per group on hosted, global otherwise
    private Integer initialRedundancy = null;
    private Integer finalRedundancy = null;
    private Integer readyCopies = null;

    // Always global (across groups)
    private Integer globalMinRedundancy = null;

    public RedundancyBuilder(ModelElement clusterXml) {
        readyCopies = clusterXml.childAsInteger("engine.proton.searchable-copies");

        ModelElement redundancyElement = clusterXml.child("redundancy");
        if (redundancyElement != null) {
            initialRedundancy = redundancyElement.integerAttribute("reply-after");
            finalRedundancy = (int) redundancyElement.asLong();

            if (initialRedundancy == null) {
                initialRedundancy = finalRedundancy;
            } else {
                if (finalRedundancy < initialRedundancy) {
                    throw new IllegalArgumentException("Final redundancy must be higher than or equal to initial redundancy");
                }
            }

            if (readyCopies != null && readyCopies > finalRedundancy)
                throw new IllegalArgumentException("Number of searchable copies can not be higher than final redundancy");
        }

        ModelElement minRedundancyElement = clusterXml.child("min-redundancy");
        if (minRedundancyElement != null) {
            globalMinRedundancy = (int)minRedundancyElement.asLong();
        }

        if (redundancyElement == null && minRedundancyElement == null)
            throw new IllegalArgumentException("Either <redundancy> or <min-redundancy> must be set");
    }

    /**
     * @param isHosted
     * @param isStreaming true if this cluster only has schemas with streaming mode, false if it only has schemas
     *                    without streaming, null if it has both
     * @param subGroups
     * @param leafGroups
     * @param totalNodes
     * @return
     */
    public Redundancy build(boolean isHosted, Boolean isStreaming, int subGroups, int leafGroups,  int totalNodes) {
        if (isHosted) {
            if (globalMinRedundancy != null && ( finalRedundancy == null || finalRedundancy * leafGroups < globalMinRedundancy ))
                initialRedundancy = finalRedundancy = (int)Math.ceil((double)globalMinRedundancy / leafGroups);
            if (readyCopies == null) {
                if (isStreaming == Boolean.TRUE) {
                    readyCopies = finalRedundancy;
                }
                else { // If isStreaming is null (mixed mode cluster) there are no good options ...
                    if (leafGroups > 1)
                        readyCopies = 1;
                    else
                        readyCopies = finalRedundancy > 1 ? 2 : 1;
                }
            }
            else {
                readyCopies = Math.min(readyCopies, finalRedundancy);
            }
            return new Redundancy(initialRedundancy, finalRedundancy, readyCopies, leafGroups, totalNodes);
        } else {
            if (globalMinRedundancy != null && ( finalRedundancy == null || finalRedundancy < globalMinRedundancy))
                initialRedundancy = finalRedundancy = globalMinRedundancy;
            if (readyCopies == null) {
                if (isStreaming == Boolean.TRUE)
                    readyCopies = finalRedundancy;
                else // If isStreaming is null (mixed mode cluster) there are no good options ...
                    readyCopies = finalRedundancy > 1 ? Math.max(subGroups, 2) : 1;
            }
            subGroups = Math.max(1, subGroups);
            IndexedHierarchicDistributionValidator.validateThatLeafGroupsCountIsAFactorOfRedundancy(finalRedundancy, subGroups);
            IndexedHierarchicDistributionValidator.validateThatReadyCopiesIsCompatibleWithRedundancy(finalRedundancy, readyCopies, subGroups);
            if (initialRedundancy == null)
                initialRedundancy = finalRedundancy;
            return new Redundancy(initialRedundancy/subGroups, finalRedundancy/subGroups,
                                  readyCopies/subGroups, subGroups, totalNodes);
        }
    }

}
