/*
 * Copyright (c) 2019 The American National Corpus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.anc.lapps.vocab.dsl

import org.junit.Ignore
import org.junit.Test

/**
 *
 */
@Ignore
class VocabDslTest {

    @Test
    void generateTTL() {
        String[] args = "-r ttl -v 1.0.0 src/test/resources/lapps.vocab".split()
        VocabDsl.main(args)
    }

    @Test
    void generateIdrefs() {
        VocabDsl.main("-r ttl -v 1.0.0 src/test/resources/idref.vocab".split())
    }

    @Test
    void generateXSD() {
        String[] args = "--xsd src/test/resources/lapps.vocab".split()
        VocabDsl.main(args)
    }

    @Test
    void generateExampleVocabulary() {
        ['ttl', 'owl', 'rdf', 'jsonld'].each { String format ->
            String[] args = "-r $format src/test/resources/example.vocabulary".split()
            VocabDsl.main(args)
        }
    }

    @Test
    void generateExampleTTL() {
        String[] args = "-r ttl src/test/resources/example.vocabulary".split()
        VocabDsl.main(args)
    }

    @Test
    void generateAll() {
        ['ttl', 'owl', 'rdf', 'jsonld'].each { String format ->
            String[] args = "-r $format src/test/resources/lapps.vocab".split()
            VocabDsl.main(args)
        }
    }

    @Test
    void generateSchema() {
        VocabDsl.main("-x src/test/resources/lapps.vocab".split())
    }

}
