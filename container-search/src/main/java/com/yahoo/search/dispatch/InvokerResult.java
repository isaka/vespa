// Copyright Vespa.ai. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.search.dispatch;

import com.yahoo.prelude.fastsearch.FastHit;
import com.yahoo.search.Query;
import com.yahoo.search.Result;
import com.yahoo.search.query.Sorting;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps a Result and a flat, skinny hit list
 *
 * @author baldersheim
 */
public class InvokerResult {

    private final Result result;
    private final List<LeanHit> leanHits;

    public InvokerResult(Result result) {
        this.result = result;
        this.leanHits = List.of();
    }

    public InvokerResult(Query query, int expectedHits) {
        result = new Result(query);
        leanHits = new ArrayList<>(expectedHits);
    }

    public Result getResult() {
        return result;
    }

    public List<LeanHit> getLeanHits() {
        return leanHits;
    }

    void complete() {
        Query query = result.getQuery();
        Sorting sorting = query.getRanking().getSorting();
        for (LeanHit hit : leanHits) {
            FastHit fh = new FastHit(hit.getGid(), hit.getRelevance(), hit.getPartId(), hit.getDistributionKey());
            if (hit.hasSortData()) {
                fh.setSortData(hit.getSortData(), sorting);
            }
            fh.setQuery(query);
            fh.setFillable();
            if (hit.hasMatchFeatures()) {
                fh.setField("matchfeatures", hit.getMatchFeatures());
                fh.setFilled("[f:matchfeatures]");
            }
            fh.setCached(false);
            result.hits().add(fh);
        }
        if (!leanHits.isEmpty())
            leanHits.clear();
    }

}
