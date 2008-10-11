package org.apache.maven.plugin.coreit;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.util.Collection;
import java.util.Iterator;

/**
 * Installs the project artifacts into the local repository. This is the essence of the Maven Install Plugin.
 * <strong>Note:</strong> Unlike the production plugin, this plugin does not handle projects with "pom" packaging for
 * the sake of simplicity.
 * 
 * @goal install
 * @phase install
 * 
 * @author Benjamin Bentmann
 * @version $Id$
 */
public class InstallMojo
    extends AbstractMojo
{

    /**
     * The project's main artifact.
     * 
     * @parameter default-value="${project.artifact}"
     * @readonly
     * @required
     */
    private Artifact mainArtifact;

    /**
     * The project's attached artifact.
     * 
     * @parameter default-value="${project.attachedArtifacts}"
     * @readonly
     * @required
     */
    private Collection attachedArtifacts;

    /**
     * The local repository.
     * 
     * @parameter default-value="${localRepository}"
     * @readonly
     * @required
     */
    private ArtifactRepository localRepository;

    /**
     * The artifact installer.
     * 
     * @component
     */
    private ArtifactInstaller installer;

    /**
     * Runs this mojo.
     * 
     * @throws MojoExecutionException If any artifact could not be installed.
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        getLog().info( "[MAVEN-CORE-IT-LOG] Installing project artifacts" );

        try
        {
            installer.install( mainArtifact.getFile(), mainArtifact, localRepository );

            if ( attachedArtifacts != null )
            {
                for ( Iterator it = attachedArtifacts.iterator(); it.hasNext(); )
                {
                    Artifact attachedArtifact = (Artifact) it.next();
                    installer.install( attachedArtifact.getFile(), attachedArtifact, localRepository );
                }
            }
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Failed to install artifacts", e );
        }
    }

}