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

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * This is a sample integration test. The IT tests typically
 * operate by having a sample project in the
 * /src/test/resources folder along with a junit test like
 * this one. The junit test uses the verifier (which uses
 * the invoker) to invoke a new instance of Maven on the
 * project in the resources folder. It then checks the
 * results. This is a non-trivial example that shows two
 * phases. See more information inline in the code.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 *
 */
public class MavenITmng3415JunkRepositoryMetadataTest
    extends AbstractMavenIntegrationTestCase
{
    private static final String RESOURCE_BASE = "/mng-3415";

    public MavenITmng3415JunkRepositoryMetadataTest()
    {
        // we're going to control the test execution according to the maven version present within each test method.
        // all methods should execute as long as we're using maven 2.0.9+, but the specific tests may vary a little
        // depending on which version we're using above 2.0.8.
        super( "(2.0.8,)" ); // only test in 2.0.9+
    }

    /**
     * This test simply verifies that when a metadata transfer fails (network error, etc.)
     * no metadata file is written to the local repository.
     *
     * Steps executed to verify this test:
     *
     * 0. Find the local repository directory:
     *    a. build the maven-find-local-repo-plugin, then run it, to spit out the path of the
     *       local repository in use by default. Read the output file to get this path.
     *       (Yes, it's heavy, but it's reliable.)
     * 1. Setup the test:
     *    a. Make sure the metadata for the test-repo is NOT in the local repository.
     *    b. Make sure the dependency POM IS in the local repository, so we're not
     *       distracted by failed builds that are unrelated.
     *    c. Create the settings file for use in this test, which contains the invalid
     *       remote repository entry.
     * 2. Build the test project the first time
     *    a. Verify that a TransferFailedException is in the build output for the test-repo
     *    b. Verify that the metadata for the dependency POM is NOT in the local
     *       repository afterwards.
     * 3. Build the test project the second time
     *    a. See (2.a) and (2.b) above; the same criteria applies here.
     */
    public void testitMNG3415()
        throws Exception
    {
        String methodName = getMethodName();

        File testDir = ResourceExtractor.simpleExtractResources( getClass(), RESOURCE_BASE );
        File projectDir = new File( testDir, "project" );

        File logFile = new File( projectDir, "log.txt" );

        File localRepo = findLocalRepoDirectory();

        setupDummyDependency( testDir, localRepo, true );

        Verifier verifier;

        verifier = new Verifier( projectDir.getAbsolutePath() );

        Properties filterProps = verifier.newDefaultFilterProperties();
        filterProps.put( "@baseurl@", "invalid" + filterProps.getProperty( "@baseurl@" ).substring( "file".length() ) );
        File settings = verifier.filterFile( "../settings-template.xml", "settings-a.xml", "UTF-8", filterProps );

        List cliOptions = new ArrayList();
        cliOptions.add( "-X" );
        cliOptions.add( "-s" );
        cliOptions.add( settings.getPath() );

        verifier.setCliOptions( cliOptions );
        verifier.setLogFileName( "log-" + methodName + "-firstBuild.txt" );
        verifier.executeGoal( "package" );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        assertMetadataMissing( localRepo );

        setupDummyDependency( testDir, localRepo, true );

        verifier.setLogFileName( "log-" + methodName + "-secondBuild.txt" );
        verifier.executeGoal( "package" );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        assertMetadataMissing( localRepo );
    }

    private String getMethodName()
    {
        return new Throwable().getStackTrace()[1].getMethodName();
    }

    /**
     * This test simply verifies that when metadata doesn't exist on the remote
     * repository, a basic metadata file is written to the local repository.
     *
     * Steps executed to verify this test:
     *
     * 0. Find the local repository directory:
     *    a. build the maven-find-local-repo-plugin, then run it, to spit out the path of the
     *       local repository in use by default. Read the output file to get this path.
     *       (Yes, it's heavy, but it's reliable.)
     * 1. Setup the test:
     *    a. Make sure the metadata for the test-repo is NOT in the local repository.
     *    b. Make sure the dependency POM IS in the local repository, so we're not
     *       distracted by failed builds that are unrelated.
     *    c. Create the settings file for use in this test, which contains the VALID
     *       remote repository entry.
     * 2. Build the test project the first time
     *    a. Verify that a log message checking the remote repository for the metadata file
     *       is in the build output for the test-repo
     * 3. Build the test project the second time
     *    a. Verify that a log message checking the remote repository for the metadata file
     *       IS NOT in the build output for the test-repo
     *    b. Verify that the file used for updateInterval calculations was NOT changed from
     *       the first build.
     *
     * @fixme: Find a better mechanism for testing this than matching console output!
     */
    public void testShouldNotRepeatedlyUpdateOnResourceNotFoundException()
        throws Exception
    {
        String methodName = getMethodName();

        File testDir = ResourceExtractor.simpleExtractResources( getClass(), RESOURCE_BASE );
        File projectDir = new File( testDir, "project" );

        File localRepo = findLocalRepoDirectory();

        setupDummyDependency( testDir, localRepo, true );

        Verifier verifier;

        verifier = new Verifier( projectDir.getAbsolutePath() );

        Properties filterProps = verifier.newDefaultFilterProperties();
        File settings = verifier.filterFile( "../settings-template.xml", "settings-b.xml", "UTF-8", filterProps );

        List cliOptions = new ArrayList();
        cliOptions.add( "-X" );
        cliOptions.add( "-s" );
        cliOptions.add( settings.getPath() );

        verifier.setCliOptions( cliOptions );

        verifier.setLogFileName( "log-" + methodName + "-firstBuild.txt" );
        verifier.executeGoal( "package" );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        File firstLogFile = new File( projectDir, verifier.getLogFileName() );

        // FIXME: There really should be a better way than this!
        assertOutputLinePresent( verifier, firstLogFile, "snapshot tests:missing:1.0-SNAPSHOT: checking for updates from testing-repo" );

        File updateCheckFile = getUpdateCheckFile( localRepo );
        long firstLastMod = updateCheckFile.lastModified();

        setupDummyDependency( testDir, localRepo, false );

        verifier.setLogFileName( "log-" + methodName + "-secondBuild.txt" );
        verifier.executeGoal( "package" );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        File secondLogFile = new File( projectDir, verifier.getLogFileName() );

        // FIXME: There really should be a better way than this!
        assertOutputLineMissing( verifier, secondLogFile, "snapshot tests:missing:1.0-SNAPSHOT: checking for updates from testing-repo" );

        assertEquals( "Last-modified time should be unchanged from first build through second build for the file we use for updateInterval checks.", firstLastMod, updateCheckFile.lastModified() );
    }

    private void assertMetadataMissing( File localRepo )
    {
        File metadata = getMetadataFile( localRepo );

        assertFalse( "Metadata file should NOT be present in local repository: "
                     + metadata.getAbsolutePath(), metadata.exists() );
    }

    private void setupDummyDependency( File testDir,
                                       File localRepo,
                                       boolean resetUpdateInterval )
        throws VerificationException, IOException
    {
        File metadata = getMetadataFile( localRepo );

        if ( resetUpdateInterval && metadata.exists() )
        {
            System.out.println( "Deleting metadata file: " + metadata );
            metadata.delete();
        }

        File resolverStatus = new File( metadata.getParentFile(), "resolver-status.properties" );
        if ( resetUpdateInterval && resolverStatus.exists() )
        {
            System.out.println( "Deleting resolver-status.properties file related to: " + metadata );
            resolverStatus.delete();
        }

        File dir = metadata.getParentFile();

        System.out.println( "Setting up dependency POM in: " + dir );

        File pom = new File( dir, "missing-1.0-SNAPSHOT.pom" );

        if ( pom.exists() )
        {
            System.out.println( "Deleting pre-existing POM: " + pom );
            pom.delete();
        }

        File pomSrc = new File( testDir, "dependency-pom.xml" );

        System.out.println( "Copying dependency POM\nfrom: " + pomSrc + "\nto: " + pom );

        FileUtils.copyFile( pomSrc, pom );
    }

    private File getMetadataFile( File localRepo )
    {
        File dir = new File( localRepo, "tests/missing/1.0-SNAPSHOT" );

        dir.mkdirs();

        return new File( dir, "maven-metadata-testing-repo.xml" );
    }

    /**
     * If the current maven version is < 3.0, we'll use the metadata file itself (old maven-artifact code)...
     * otherwise, use the new resolver-status.properties file (new artifact code).
     */
    private File getUpdateCheckFile( File localRepo )
    {
        File dir = new File( localRepo, "tests/missing/1.0-SNAPSHOT" );

        dir.mkdirs();

        // < 3.0 (including snapshots)
        if ( matchesVersionRange( "(2.0.8,2.999)" ) )
        {
            return new File( dir, "maven-metadata-testing-repo.xml" );
        }
        else
        {
            return new File( dir, "resolver-status.properties" );
        }
    }

    private File findLocalRepoDirectory()
        throws VerificationException, IOException
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(),
                                                                 RESOURCE_BASE
                                                                                 + "/maven-find-local-repo-plugin" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );

        verifier.deleteArtifact( "org.apache.maven.plugins", "maven-find-local-repo-plugin", "1.0-SNAPSHOT", "jar" );

        verifier.executeGoal( "install" );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.executeGoal( "find-local-repo:find" );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List lines = verifier.loadFile( new File( testDir, "target/local-repository-location.txt" ),
                                        false );

        File localRepo = new File( (String) lines.get( 0 ) );

        System.out.println( "Using local repository at: " + localRepo );

        return localRepo;
    }

    private void assertOutputLinePresent( Verifier verifier,
                                   File logFile,
                                   String lineContents )
        throws VerificationException
    {
        List lines = verifier.loadFile( logFile, false );

        boolean found = false;
        for ( Iterator it = lines.iterator(); it.hasNext(); )
        {
            String line = (String) it.next();
            if ( line.indexOf( lineContents ) > -1 )
            {
                found = true;
                break;
            }
        }

        assertTrue( "Build output in:\n\n" + logFile + "\n\nshould contain line with contents:\n\n" + lineContents + "\n", found );
    }

    private void assertOutputLineMissing( Verifier verifier,
                                   File logFile,
                                   String lineContents )
        throws VerificationException
    {
        List lines = verifier.loadFile( logFile, false );

        boolean found = false;
        for ( Iterator it = lines.iterator(); it.hasNext(); )
        {
            String line = (String) it.next();
            if ( line.indexOf( lineContents ) > -1 )
            {
                found = true;
                break;
            }
        }

        assertFalse( "Build output in:\n\n" + logFile + "\n\nshould NOT contain line with contents:\n\n" + lineContents + "\n", found );
    }

}
