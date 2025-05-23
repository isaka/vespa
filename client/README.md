<!-- Copyright Vespa.ai. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root. -->

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="https://assets.vespa.ai/logos/Vespa-logo-green-RGB.svg">
  <source media="(prefers-color-scheme: light)" srcset="https://assets.vespa.ai/logos/Vespa-logo-dark-RGB.svg">
  <img alt="#Vespa" width="200" src="https://assets.vespa.ai/logos/Vespa-logo-dark-RGB.svg" style="margin-bottom: 25px;">
</picture>

# Vespa clients

This part of the Vespa repository got Vespa client implementations for operations like

- deploy
- read/write
- query

<!-- ToDo: illustration -->

## Vespa CLI

The Vespa command-line tool, see the [README](go/README.md).
Use the Vespa CLI to deploy, feed and query a Vespa application,
for local, self-hosted or [Vespa Cloud](https://vespa.ai/free-trial/) instances.

## pyvespa

[pyvespa](https://pyvespa.readthedocs.io/en/latest/) provides a python API to Vespa -
use it to create, modify, deploy and interact with running Vespa instances.
The main pyvespa goal is to allow for faster prototyping
and to facilitate Machine Learning experiments for Vespa applications.

## Vespa FE (fixme: better name and description here)

This is a [work-in-progress javascript app](js/app) for querying a Vespa application.

---

## Misc

<!-- ToDo: move this / demote this somehow -->

### vespa\_query\_dsl

This lib is used for composing Vespa
[YQL queries](https://docs.vespa.ai/en/reference/query-language-reference.html).
For usage, refer to the [QTest.java](src/test/java/ai/vespa/client/dsl/QTest.java) unit test.

ToDos:

- [ ] support `predicate` (https://docs.vespa.ai/en/predicate-fields.html)
- [ ] support methods for checking positive/negative conditions for specific field
- [x] support order by annotation
- [x] support order by
- [x] support sub operators in contains (sameElement, phrase, near, onear, equiv)
- [x] support group syntax
- [x] support `nonEmpty`
- [x] support `dotProduct`
- [x] support `weightedSet`
- [x] support `wand`
- [x] support `weakAnd`
- [x] support `userInput`
- [x] support `rank`
- [x] support filter annotation
- [x] unit tests
- [x] support other annotations
- [x] handle edge cases (e.g. `Q.b("test").contains("a").build()`)
