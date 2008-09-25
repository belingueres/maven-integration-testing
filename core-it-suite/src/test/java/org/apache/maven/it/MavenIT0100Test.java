package org.apache.maven.it;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MavenIT0100Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test that ${parent.artifactId} resolves correctly. [MNG-2124]
     */
    public void testit0100()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0100" );
        File child = new File( testDir, "parent/child" );

        Verifier verifier = new Verifier( child.getAbsolutePath() );

        List options = new ArrayList();
        options.add( "-Doutput=\"" + new File( child, "target/effective-pom.txt" ).getAbsolutePath() + "\"" );

        verifier.setCliOptions( options );

        List goals = new ArrayList();
        goals.add( "org.apache.maven.plugins:maven-help-plugin:2.0.2:effective-pom" );
        goals.add( "verify" );

        verifier.executeGoals( goals );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}
