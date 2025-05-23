// Copyright Vespa.ai. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.documentmodel;

import com.yahoo.config.application.api.DeployLogger;
import com.yahoo.schema.Schema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.logging.Level.WARNING;

/**
 * A document summary definition - a list of summary fields.
 *
 * @author bratseth
 */
public class DocumentSummary {

    private final String name;
    private final Map<String, SummaryField> fields = new LinkedHashMap<>();

    private boolean fromDisk = false;
    private boolean omitSummaryFeatures = false;
    private final List<String> inherited = new ArrayList<>();

    private final Schema owner;

    /** Creates a DocumentSummary with the given name. */
    public DocumentSummary(String name, Schema owner) {
        this.name = name;
        this.owner = owner;
    }

    public String name()                     { return name; }
    public Schema owner()                    { return owner; }
    public Collection<SummaryField> values() { return fields.values(); }
    public SummaryField get(String name)     { return fields.get(name); }
    public void remove(String name)          { fields.remove(name); }

    /**
     * Adds a field to this.
     * The model is constrained to ensure that summary fields of the same name
     * in different classes have the same summary transform, because this is
     * what is supported by the backend currently.
     *
     * @param field the summary field to add
     * @return this for chaining
     */
    public DocumentSummary add(SummaryField field) {
        field.addDestination(name());
        if (fields.containsKey(field.getName())) {
            if ( ! fields.get(field.getName()).equals(field)) {
                throw new IllegalArgumentException("Duplicate declaration of " + field + " in " + this);
            }
        } else {
            fields.put(field.getName(), field);
        }
        return this;
    }

    /**
     * Adds another set of fields to this.
     *
     * @param other the fields to be added to this
     * @return this for chaining
     */
    public DocumentSummary add(DocumentSummary other) {
        for(var field : other.values())
            add(field);
        return this;
    }

    public void setFromDisk(boolean fromDisk) { this.fromDisk = fromDisk; }

    /** Returns whether the user has noted explicitly that this summary accesses disk */
    public boolean isFromDisk() { return fromDisk; }

    public void setOmitSummaryFeatures(boolean value) {
        omitSummaryFeatures = value;
    }

    public boolean omitSummaryFeatures() {
        return omitSummaryFeatures;
    }

    public SummaryField getSummaryField(String name) {
        var field = get(name);
        if (field != null) return field;
        if (inherited().isEmpty()) return null;
        for (var inheritedSummary : inherited()) {
            var inheritedField = inheritedSummary.getSummaryField(name);
            if (inheritedField != null)
                return inheritedField;
        }
        return null;
    }

    public Map<String, SummaryField> getSummaryFields() {
        var allFields = new LinkedHashMap<String, SummaryField>(values().size());
        for (var inheritedSummary : inherited()) {
            if (inheritedSummary == null) continue;
            allFields.putAll(inheritedSummary.getSummaryFields());
        }
        for (var field : values())
            allFields.put(field.getName(), field);
        return allFields;
    }

    /**
     * Removes implicit fields which shouldn't be included.
     * This is implicitly added fields which are sources for
     * other fields. We then assume they are not intended to be added
     * implicitly in addition.
     * This should be called when this summary is complete.
     */
    public void purgeImplicits() {
        List<SummaryField> falseImplicits = new ArrayList<>();
        for (SummaryField summaryField : getSummaryFields().values()) {
            if (summaryField.isImplicit()) continue;
            for (Iterator<SummaryField.Source> j = summaryField.sourceIterator(); j.hasNext(); ) {
                String sourceName = j.next().getName();
                if (sourceName.equals(summaryField.getName())) continue;
                SummaryField sourceField=getSummaryField(sourceName);
                if (sourceField == null) continue;
                if (!sourceField.isImplicit()) continue;
                falseImplicits.add(sourceField);
            }
        }
        for (SummaryField field : falseImplicits) {
            remove(field.getName());
        }
    }

    /** Adds a parent of this. Both summaries must be present in the same schema, or a parent schema. */
    public void addInherited(String inherited) {
        this.inherited.add(inherited);
    }

    /** Returns the parent of this, if any */
    public List<DocumentSummary> inherited() {
        return inherited.stream().map(owner::getSummary).toList();
    }

    @Override
    public String toString() {
        return "document-summary '" + name() + "' in " + owner;
    }

    public void validate(DeployLogger logger) {
        for (var inheritedName : inherited) {
            var inheritedSummary = owner.getSummary(inheritedName);
            // TODO: Throw when no one is doing this anymore
            if (inheritedName.equals("default"))
                logger.logApplicationPackage(WARNING, this + " inherits '" + inheritedName +
                                                      "', which makes no sense. Remove this inheritance");
            else if (inheritedSummary == null )
                throw new IllegalArgumentException(this + " inherits '" + inheritedName +
                                                   "', but this is not present in " + owner);
        }
    }

}
