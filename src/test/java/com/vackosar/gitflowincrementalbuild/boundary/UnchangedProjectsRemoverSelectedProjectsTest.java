package com.vackosar.gitflowincrementalbuild.boundary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vackosar.gitflowincrementalbuild.control.Property;

/**
 * Tests {@link UnchangedProjectsRemover} with Mockito mocks when "selected projects" are present ({@code mvn -pl ...}).
 *
 * @author famod
 */
public class UnchangedProjectsRemoverSelectedProjectsTest extends BaseUnchangedProjectsRemoverTest {

    @BeforeEach
    void beforeThis() throws GitAPIException, IOException {
        addGibProperty(Property.skipTestsForUpstreamModules, "true");
    }

    // mvn -pl :module-B
    @Test
    public void nothingChanged() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, false);
        setProjectSelections(moduleB);
        overrideProjects(moduleB);

        underTest.act(config());

        assertThat(mavenSessionMock.getGoals()).as("Unexpected goals").isEqualTo(Collections.emptyList());

        verify(mavenSessionMock, never()).setProjects(anyList());

        assertProjectPropertiesEqual(moduleB);
    }

    // mvn -pl :module-B
    @Test
    public void nothingChanged_disableSelectedProjectsHandling() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, false);
        setProjectSelections(moduleB);
        overrideProjects(moduleB);

        addGibProperty(Property.disableSelectedProjectsHandling, "true");

        underTest.act(config());

        assertThat(mavenSessionMock.getGoals()).as("Unexpected goals").isEqualTo(Collections.singletonList("validate"));

        verify(mavenSessionMock).setProjects(Collections.singletonList(moduleB));

        assertProjectPropertiesEqual(moduleB);
    }


    // mvn -pl :module-B,:module-C
    @Test
    public void nothingChanged_twoSelected() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, false);
        MavenProject moduleC = addModuleMock(AID_MODULE_C, false);
        setProjectSelections(moduleB, moduleC);
        overrideProjects(moduleB, moduleC);

        underTest.act(config());

        assertThat(mavenSessionMock.getGoals()).as("Unexpected goals").isEqualTo(Collections.emptyList());

        verify(mavenSessionMock, never()).setProjects(anyList());

        assertProjectPropertiesEqual(moduleB);
        assertProjectPropertiesEqual(moduleC);
    }

    // mvn -pl :module-B,:module-C
    @Test
    public void nothingChanged_twoSelected_disableSelectedProjectsHandling() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, false);
        MavenProject moduleC = addModuleMock(AID_MODULE_C, false);
        setProjectSelections(moduleB, moduleC);
        overrideProjects(moduleB, moduleC);

        addGibProperty(Property.disableSelectedProjectsHandling, "true");

        underTest.act(config());

        assertThat(mavenSessionMock.getGoals()).as("Unexpected goals").isEqualTo(Collections.singletonList("validate"));

        verify(mavenSessionMock).setProjects(Collections.singletonList(moduleB));   // only B (sic!), as there can only be one "current" project

        assertProjectPropertiesEqual(moduleB);
        assertProjectPropertiesEqual(moduleC);
    }

    // mvn -pl :module-B -am
    @Test
    public void nothingChanged_makeUpstream() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, false);
        setProjectSelections(moduleB);
        when(mavenExecutionRequestMock.getMakeBehavior()).thenReturn(MavenExecutionRequest.REACTOR_MAKE_UPSTREAM);

        underTest.act(config());

        assertThat(mavenSessionMock.getGoals()).as("Unexpected goals").isEqualTo(Collections.emptyList());

        verify(mavenSessionMock, never()).setProjects(anyList());

        assertProjectPropertiesEqual(moduleA, "maven.test.skip", "true");
        assertProjectPropertiesEqual(moduleB);
    }

    // mvn -pl :module-B -am
    @Test
    public void nothingChanged_makeUpstream_buildUpstreamDisabled() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, false);
        setProjectSelections(moduleB);

        when(mavenExecutionRequestMock.getMakeBehavior()).thenReturn(MavenExecutionRequest.REACTOR_MAKE_UPSTREAM);

        addGibProperty(Property.buildUpstream, "false");

        underTest.act(config());

        assertThat(mavenSessionMock.getGoals()).as("Unexpected goals").isEqualTo(Collections.emptyList());

        verify(mavenSessionMock).setProjects(Collections.singletonList(moduleB));

        assertProjectPropertiesEqual(moduleB);
    }

    // mvn -pl :module-B -amd
    @Test
    public void nothingChanged_makeDownstream() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, false);
        MavenProject moduleC = addModuleMock(AID_MODULE_C, false);
        setUpstreamProjects(moduleC, moduleB, moduleA);
        setDownstreamProjects(moduleB, moduleC);
        setDownstreamProjects(moduleA, moduleB, moduleC);

        setProjectSelections(moduleB);
        overrideProjects(moduleB, moduleC);

        when(mavenExecutionRequestMock.getMakeBehavior()).thenReturn(MavenExecutionRequest.REACTOR_MAKE_DOWNSTREAM);

        underTest.act(config());

        assertThat(mavenSessionMock.getGoals()).as("Unexpected goals").isEqualTo(Collections.emptyList());

        // verify(mavenSessionMock).setProjects(Arrays.asList(moduleB, moduleC));
        verify(mavenSessionMock, never()).setProjects(anyList());

        assertProjectPropertiesEqual(moduleB);
        assertProjectPropertiesEqual(moduleC);
    }

    // mvn -pl :module-B -amd
    @Test
    public void nothingChanged_makeDownstream_buildDownstreamDisabled() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, false);
        MavenProject moduleC = addModuleMock(AID_MODULE_C, false);
        setUpstreamProjects(moduleC, moduleB, moduleA);
        setDownstreamProjects(moduleB, moduleC);
        setDownstreamProjects(moduleA, moduleB, moduleC);

        setProjectSelections(moduleB);
        overrideProjects(moduleB, moduleC);

        when(mavenExecutionRequestMock.getMakeBehavior()).thenReturn(MavenExecutionRequest.REACTOR_MAKE_DOWNSTREAM);

        addGibProperty(Property.buildDownstream, "false");

        underTest.act(config());

        assertThat(mavenSessionMock.getGoals()).as("Unexpected goals").isEqualTo(Collections.emptyList());

        verify(mavenSessionMock).setProjects(Collections.singletonList(moduleB));

        assertProjectPropertiesEqual(moduleB);
    }

    // mvn -pl :module-B,:module-D -amd
    // A <- B <- C
    // A <- D <- E
    @Test
    public void nothingChanged_makeDownstream_twoSelected() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, false);
        MavenProject moduleC = addModuleMock(AID_MODULE_C, false);
        setUpstreamProjects(moduleC, moduleB, moduleA);
        setDownstreamProjects(moduleB, moduleC);
        setDownstreamProjects(moduleA, moduleB, moduleC);

        MavenProject moduleD = addModuleMock(AID_MODULE_D, false);
        MavenProject moduleE = addModuleMock(AID_MODULE_E, false);
        setUpstreamProjects(moduleE, moduleD, moduleA);
        setDownstreamProjects(moduleD, moduleE);
        setDownstreamProjects(moduleA, moduleD, moduleE);

        setProjectSelections(moduleB, moduleD);
        overrideProjects(moduleB, moduleC, moduleD, moduleE);

        when(mavenExecutionRequestMock.getMakeBehavior()).thenReturn(MavenExecutionRequest.REACTOR_MAKE_DOWNSTREAM);

        underTest.act(config());

        assertThat(mavenSessionMock.getGoals()).as("Unexpected goals").isEqualTo(Collections.emptyList());

        verify(mavenSessionMock, never()).setProjects(anyList());

        assertProjectPropertiesEqual(moduleB);
        assertProjectPropertiesEqual(moduleC);
        assertProjectPropertiesEqual(moduleD);
        assertProjectPropertiesEqual(moduleE);
    }

    // mvn -pl :module-B -am -amd
    @Test
    public void nothingChanged_makeBoth() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, false);
        MavenProject moduleC = addModuleMock(AID_MODULE_C, false);
        setUpstreamProjects(moduleC, moduleB, moduleA);
        setDownstreamProjects(moduleB, moduleC);
        setDownstreamProjects(moduleA, moduleB, moduleC);

        setProjectSelections(moduleB);

        when(mavenExecutionRequestMock.getMakeBehavior()).thenReturn(MavenExecutionRequest.REACTOR_MAKE_BOTH);

        underTest.act(config());

        assertThat(mavenSessionMock.getGoals()).as("Unexpected goals").isEqualTo(Collections.emptyList());

        verify(mavenSessionMock, never()).setProjects(anyList());

        assertProjectPropertiesEqual(moduleA, "maven.test.skip", "true");
        assertProjectPropertiesEqual(moduleB);
        assertProjectPropertiesEqual(moduleC);
    }

    // mvn -pl :module-B
    @Test
    public void moduleAChanged() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, false);
        setProjectSelections(moduleB);
        overrideProjects(moduleB);

        changedProjects.add(moduleA);

        underTest.act(config());

        verify(mavenSessionMock, never()).setProjects(anyList());
    }

    // mvn -pl :module-B
    @Test
    public void moduleAChanged_disableSelectedProjectsHandling() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, false);
        setProjectSelections(moduleB);
        overrideProjects(moduleB);

        changedProjects.add(moduleA);

        addGibProperty(Property.disableSelectedProjectsHandling, "true");

        underTest.act(config());

        assertThat(mavenSessionMock.getGoals()).as("Unexpected goals").isEqualTo(Collections.emptyList());

        verify(mavenSessionMock).setProjects(Collections.singletonList(moduleB));

        assertProjectPropertiesEqual(moduleB);
    }

    // mvn -pl :module-B -am
    @Test
    public void moduleAChanged_makeUpstream() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, false);
        setProjectSelections(moduleB);
        when(mavenExecutionRequestMock.getMakeBehavior()).thenReturn(MavenExecutionRequest.REACTOR_MAKE_UPSTREAM);

        changedProjects.add(moduleA);

        underTest.act(config());

        verify(mavenSessionMock).setProjects(Arrays.asList(moduleA, moduleB));

        assertProjectPropertiesEqual(moduleA, "maven.test.skip", "true");
        assertProjectPropertiesEqual(moduleB);
    }

    // mvn -pl :module-B -am
    @Test
    public void moduleAChanged_makeUpstream_buildUpstreamDisabled() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, false);
        setProjectSelections(moduleB);
        when(mavenExecutionRequestMock.getMakeBehavior()).thenReturn(MavenExecutionRequest.REACTOR_MAKE_UPSTREAM);

        changedProjects.add(moduleA);

        addGibProperty(Property.buildUpstream, "false");

        underTest.act(config());

        verify(mavenSessionMock).setProjects(Collections.singletonList(moduleB));

        assertProjectPropertiesEqual(moduleB);
    }

    // mvn -pl :module-B -am
    @Test
    public void moduleAChanged_makeUpstream_moduleCSelected() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, false);
        MavenProject moduleC = addModuleMock(AID_MODULE_C, false);
        setDownstreamProjects(moduleB, moduleC);
        setUpstreamProjects(moduleC, moduleA, moduleB);

        setProjectSelections(moduleC);

        when(mavenExecutionRequestMock.getMakeBehavior()).thenReturn(MavenExecutionRequest.REACTOR_MAKE_UPSTREAM);

        changedProjects.add(moduleA);

        underTest.act(config());

        // upstream B was not changed but its upstream A, so B must be built as well
        verify(mavenSessionMock).setProjects(Arrays.asList(moduleA, moduleB, moduleC));

        assertProjectPropertiesEqual(moduleA, "maven.test.skip", "true");
        assertProjectPropertiesEqual(moduleB, "maven.test.skip", "true");
        assertProjectPropertiesEqual(moduleC);
    }

    // mvn -pl :module-B -am -amd
    @Test
    public void moduleAChanged_makeBoth() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, false);
        MavenProject moduleC = addModuleMock(AID_MODULE_C, false);
        setUpstreamProjects(moduleC, moduleB, moduleA);
        setDownstreamProjects(moduleB, moduleC);
        setDownstreamProjects(moduleA, moduleB, moduleC);

        setProjectSelections(moduleB);

        when(mavenExecutionRequestMock.getMakeBehavior()).thenReturn(MavenExecutionRequest.REACTOR_MAKE_BOTH);

        changedProjects.add(moduleA);

        underTest.act(config());

        verify(mavenSessionMock).setProjects(Arrays.asList(moduleA, moduleB, moduleC));

        assertProjectPropertiesEqual(moduleA, "maven.test.skip", "true");
        assertProjectPropertiesEqual(moduleB);
        assertProjectPropertiesEqual(moduleC);
    }

    // mvn -pl :module-B -am -amd
    @Test
    public void moduleAChanged_makeBoth_buildUpstreamDisabled() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, false);
        MavenProject moduleC = addModuleMock(AID_MODULE_C, false);
        setUpstreamProjects(moduleC, moduleB, moduleA);
        setDownstreamProjects(moduleB, moduleC);
        setDownstreamProjects(moduleA, moduleB, moduleC);

        setProjectSelections(moduleB);

        when(mavenExecutionRequestMock.getMakeBehavior()).thenReturn(MavenExecutionRequest.REACTOR_MAKE_BOTH);

        addGibProperty(Property.buildUpstream, "false");

        changedProjects.add(moduleA);

        underTest.act(config());

        verify(mavenSessionMock).setProjects(Arrays.asList(moduleB, moduleC));

        assertProjectPropertiesEqual(moduleB);
        assertProjectPropertiesEqual(moduleC);
    }

    // mvn -pl :module-B -am -amd
    @Test
    public void moduleAChanged_makeBoth_buildDownstreamDisabled() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, false);
        MavenProject moduleC = addModuleMock(AID_MODULE_C, false);
        setUpstreamProjects(moduleC, moduleB, moduleA);
        setDownstreamProjects(moduleB, moduleC);
        setDownstreamProjects(moduleA, moduleB, moduleC);

        setProjectSelections(moduleB);

        when(mavenExecutionRequestMock.getMakeBehavior()).thenReturn(MavenExecutionRequest.REACTOR_MAKE_BOTH);

        addGibProperty(Property.buildDownstream, "false");

        changedProjects.add(moduleA);

        underTest.act(config());

        verify(mavenSessionMock).setProjects(Arrays.asList(moduleA, moduleB));

        assertProjectPropertiesEqual(moduleA, "maven.test.skip", "true");
        assertProjectPropertiesEqual(moduleB);
    }

    // mvn -pl :module-B -am -amd
    @Test
    public void moduleAChanged_makeDownstream_buildDownstreamDisabled() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, false);
        MavenProject moduleC = addModuleMock(AID_MODULE_C, false);
        setUpstreamProjects(moduleC, moduleB, moduleA);
        setDownstreamProjects(moduleB, moduleC);
        setDownstreamProjects(moduleA, moduleB, moduleC);

        setProjectSelections(moduleB);
        overrideProjects(moduleA, moduleB);

        when(mavenExecutionRequestMock.getMakeBehavior()).thenReturn(MavenExecutionRequest.REACTOR_MAKE_DOWNSTREAM);

        addGibProperty(Property.buildDownstream, "false");

        changedProjects.add(moduleA);

        underTest.act(config());

        verify(mavenSessionMock).setProjects(Collections.singletonList(moduleB));

        assertProjectPropertiesEqual(moduleB);
    }

    // mvn -pl !:module-A
    @Test
    public void moduleAChanged_deselectedA() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, false);
        MavenProject moduleC = addModuleMock(AID_MODULE_C, false);
        setUpstreamProjects(moduleC, moduleB, moduleA);
        setDownstreamProjects(moduleB, moduleC);
        setDownstreamProjects(moduleA, moduleB, moduleC);

        setProjectDeSelections(moduleA);
        overrideProjects(moduleB, moduleC);

        changedProjects.add(moduleA);

        underTest.act(config());

        verify(mavenSessionMock).setProjects(Arrays.asList(moduleB, moduleC));
    }

    // mvn -pl !:module-A
    @Test
    public void moduleAChanged_deselectedA_buildDownstreamDisabled() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, false);
        MavenProject moduleC = addModuleMock(AID_MODULE_C, false);
        setUpstreamProjects(moduleC, moduleB, moduleA);
        setDownstreamProjects(moduleB, moduleC);
        setDownstreamProjects(moduleA, moduleB, moduleC);

        setProjectDeSelections(moduleA);
        overrideProjects(moduleB, moduleC);

        addGibProperty(Property.buildDownstream, "false");

        changedProjects.add(moduleA);

        underTest.act(config());

        verify(mavenSessionMock).setProjects(Collections.singletonList(moduleB));
    }

    // mvn -pl !:module-A
    @Test
    public void moduleAChanged_deselectedA_buildDownstreamDisabled_buildAllIfNoChanges() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, false);
        MavenProject moduleC = addModuleMock(AID_MODULE_C, false);
        setUpstreamProjects(moduleC, moduleB, moduleA);
        setDownstreamProjects(moduleB, moduleC);
        setDownstreamProjects(moduleA, moduleB, moduleC);

        setProjectDeSelections(moduleA);
        overrideProjects(moduleB, moduleC);

        addGibProperty(Property.buildDownstream, "false");
        addGibProperty(Property.buildAllIfNoChanges, "true");

        changedProjects.add(moduleA);

        underTest.act(config());

        verify(mavenSessionMock, never()).setProjects(anyList());

        assertProjectPropertiesEqual(moduleB, "maven.test.skip", "true");
        assertProjectPropertiesEqual(moduleC, "maven.test.skip", "true");
    }

    // mvn -pl :module-B
    @Test
    public void moduleBChanged() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, true);
        setProjectSelections(moduleB);
        overrideProjects(moduleB);

        underTest.act(config());

        verify(mavenSessionMock, never()).setProjects(anyList());
    }

    @Test
    public void moduleBChanged_disableSelectedProjectsHandling() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, true);
        setProjectSelections(moduleB);
        overrideProjects(moduleB);

        addGibProperty(Property.disableSelectedProjectsHandling, "true");

        underTest.act(config());

        verify(mavenSessionMock).setProjects(Collections.singletonList(moduleB));

        assertProjectPropertiesEqual(moduleB);
    }

    @Test
    public void moduleBChanged_twoSelected_disableSelectedProjectsHandling() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, true);
        MavenProject moduleC = addModuleMock(AID_MODULE_C, false);
        setProjectSelections(moduleB, moduleC);
        overrideProjects(moduleB, moduleC);

        addGibProperty(Property.disableSelectedProjectsHandling, "true");

        underTest.act(config());

        verify(mavenSessionMock).setProjects(Collections.singletonList(moduleB));

        assertProjectPropertiesEqual(moduleB);
    }

    // mvn -pl :module-B -am
    @Test
    public void moduleBChanged_makeUpstream() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, true);
        setProjectSelections(moduleB);
        when(mavenExecutionRequestMock.getMakeBehavior()).thenReturn(MavenExecutionRequest.REACTOR_MAKE_UPSTREAM);

        underTest.act(config());

        verify(mavenSessionMock).setProjects(Arrays.asList(moduleA, moduleB));

        assertProjectPropertiesEqual(moduleB);
    }

    // mvn -pl :module-B -am
    @Test
    public void moduleBChanged_makeUpstream_buildAll() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, true);
        setProjectSelections(moduleB);
        when(mavenExecutionRequestMock.getMakeBehavior()).thenReturn(MavenExecutionRequest.REACTOR_MAKE_UPSTREAM);

        addGibProperty(Property.buildAll, "true");

        underTest.act(config());

        verify(mavenSessionMock, never()).setProjects(anyList());

        assertProjectPropertiesEqual(moduleA, "maven.test.skip", "true");
        assertProjectPropertiesEqual(moduleB);
    }

    // mvn -pl :module-B -am
    @Test
    public void moduleBChanged_makeUpstream_forceBuildA() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, true);
        setProjectSelections(moduleB);
        when(mavenExecutionRequestMock.getMakeBehavior()).thenReturn(MavenExecutionRequest.REACTOR_MAKE_UPSTREAM);

        addGibProperty(Property.forceBuildModules, "module-A");

        underTest.act(config());

        verify(mavenSessionMock).setProjects(Arrays.asList(moduleA, moduleB));

        assertProjectPropertiesEqual(moduleA, "maven.test.skip", "true");
        assertProjectPropertiesEqual(moduleB);
    }

    // mvn -pl :module-B -am -amd
    @Test
    public void moduleBChanged_makeBoth() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, true);
        MavenProject moduleC = addModuleMock(AID_MODULE_C, false);
        setUpstreamProjects(moduleC, moduleB, moduleA);
        setDownstreamProjects(moduleB, moduleC);
        setDownstreamProjects(moduleA, moduleB, moduleC);

        setProjectSelections(moduleB);

        when(mavenExecutionRequestMock.getMakeBehavior()).thenReturn(MavenExecutionRequest.REACTOR_MAKE_BOTH);

        underTest.act(config());

        verify(mavenSessionMock).setProjects(Arrays.asList(moduleA, moduleB, moduleC));

        assertProjectPropertiesEqual(moduleB);
        assertProjectPropertiesEqual(moduleC);
    }

    // mvn -pl !:module-B
    @Test
    public void moduleBChanged_deselectedB() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, true);
        MavenProject moduleC = addModuleMock(AID_MODULE_C, false);
        setUpstreamProjects(moduleC, moduleB, moduleA);
        setDownstreamProjects(moduleB, moduleC);
        setDownstreamProjects(moduleA, moduleB, moduleC);

        setProjectDeSelections(moduleB);
        overrideProjects(moduleA, moduleC);

        underTest.act(config());

        verify(mavenSessionMock).setProjects(Collections.singletonList(moduleC));
    }

    // mvn -pl !:module-B
    @Test
    public void moduleBChanged_deselectedB_buildDownstreamDisabled() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, true);
        MavenProject moduleC = addModuleMock(AID_MODULE_C, false);
        setUpstreamProjects(moduleC, moduleB, moduleA);
        setDownstreamProjects(moduleB, moduleC);
        setDownstreamProjects(moduleA, moduleB, moduleC);

        setProjectDeSelections(moduleB);
        overrideProjects(moduleA, moduleC);

        addGibProperty(Property.buildDownstream, "false");

        underTest.act(config());

        verify(mavenSessionMock).setProjects(Collections.singletonList(moduleA));

        assertProjectPropertiesEqual(moduleA);
    }

    // mvn -pl !:module-B
    @Test
    public void moduleBChanged_deselectedB_buildDownstreamDisabled_buildAllIfNoChanges() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, true);
        MavenProject moduleC = addModuleMock(AID_MODULE_C, false);
        setUpstreamProjects(moduleC, moduleB, moduleA);
        setDownstreamProjects(moduleB, moduleC);
        setDownstreamProjects(moduleA, moduleB, moduleC);

        setProjectDeSelections(moduleB);
        overrideProjects(moduleA, moduleC);

        addGibProperty(Property.buildDownstream, "false");
        addGibProperty(Property.buildAllIfNoChanges, "true");

        underTest.act(config());

        verify(mavenSessionMock, never()).setProjects(anyList());

        assertProjectPropertiesEqual(moduleA, "maven.test.skip", "true");
        assertProjectPropertiesEqual(moduleC, "maven.test.skip", "true");
    }

    // mvn -pl !:module-B
    @Test
    public void moduleBChanged_deselectedB_buildUpstream() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, true);
        MavenProject moduleC = addModuleMock(AID_MODULE_C, false);
        setUpstreamProjects(moduleC, moduleB, moduleA);
        setDownstreamProjects(moduleB, moduleC);
        setDownstreamProjects(moduleA, moduleB, moduleC);

        setProjectDeSelections(moduleB);
        overrideProjects(moduleA, moduleC);

        addGibProperty(Property.buildUpstream, "true");

        underTest.act(config());

        verify(mavenSessionMock).setProjects(Arrays.asList(moduleA, moduleC));

        assertProjectPropertiesEqual(moduleA, "maven.test.skip", "true");
        assertProjectPropertiesEqual(moduleC);
    }

    // mvn -pl !:module-B
    @Test
    public void moduleBChanged_deselectedB_buildUpstream_buildDownstreamDisabled() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, true);
        MavenProject moduleC = addModuleMock(AID_MODULE_C, false);
        setUpstreamProjects(moduleC, moduleB, moduleA);
        setDownstreamProjects(moduleB, moduleC);
        setDownstreamProjects(moduleA, moduleB, moduleC);

        setProjectDeSelections(moduleB);
        overrideProjects(moduleA, moduleC);

        addGibProperty(Property.buildUpstream, "true");
        addGibProperty(Property.buildDownstream, "false");

        underTest.act(config());

        verify(mavenSessionMock).setProjects(Collections.singletonList(moduleA));

        assertProjectPropertiesEqual(moduleA, "maven.test.skip", "true");
    }

    // mvn -pl :module-C,:module-E -am
    // A <- B* <- C
    // A <- D* <- E
    @Test
    public void twoSelected_differentUpstreams_bothChanged() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, true);
        MavenProject moduleC = addModuleMock(AID_MODULE_C, false);
        setUpstreamProjects(moduleC, moduleB, moduleA);
        setDownstreamProjects(moduleB, moduleC);
        setDownstreamProjects(moduleA, moduleB, moduleC);

        MavenProject moduleD = addModuleMock(AID_MODULE_D, true);
        MavenProject moduleE = addModuleMock(AID_MODULE_E, false);
        setUpstreamProjects(moduleE, moduleD, moduleA);
        setDownstreamProjects(moduleD, moduleE);
        setDownstreamProjects(moduleA, moduleD, moduleE);

        setProjectSelections(moduleC, moduleE);

        when(mavenExecutionRequestMock.getMakeBehavior()).thenReturn(MavenExecutionRequest.REACTOR_MAKE_UPSTREAM);

        underTest.act(config());

        verify(mavenSessionMock).setProjects(Arrays.asList(moduleA, moduleB, moduleC, moduleD, moduleE));

        assertProjectPropertiesEqual(moduleA, "maven.test.skip", "true");

        assertProjectPropertiesEqual(moduleB, "maven.test.skip", "true");
        assertProjectPropertiesEqual(moduleC);

        assertProjectPropertiesEqual(moduleD, "maven.test.skip", "true");
        assertProjectPropertiesEqual(moduleE);
    }

    // mvn -pl :module-C,:module-E -am
    // A <- B <- C
    // A <- D* <- E
    @Test
    public void twoSelected_differentUpstreams_oneChanged() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, false);
        MavenProject moduleC = addModuleMock(AID_MODULE_C, false);
        setUpstreamProjects(moduleC, moduleB, moduleA);
        setDownstreamProjects(moduleB, moduleC);
        setDownstreamProjects(moduleA, moduleB, moduleC);

        MavenProject moduleD = addModuleMock(AID_MODULE_D, true);
        MavenProject moduleE = addModuleMock(AID_MODULE_E, false);
        setUpstreamProjects(moduleE, moduleD, moduleA);
        setDownstreamProjects(moduleD, moduleE);
        setDownstreamProjects(moduleA, moduleD, moduleE);

        setProjectSelections(moduleC, moduleE);

        when(mavenExecutionRequestMock.getMakeBehavior()).thenReturn(MavenExecutionRequest.REACTOR_MAKE_UPSTREAM);

        underTest.act(config());

        verify(mavenSessionMock).setProjects(Arrays.asList(moduleA, moduleB, moduleC, moduleD, moduleE));

        assertProjectPropertiesEqual(moduleA, "maven.test.skip", "true");

        assertProjectPropertiesEqual(moduleB, "maven.test.skip", "true");
        assertProjectPropertiesEqual(moduleC);

        assertProjectPropertiesEqual(moduleD, "maven.test.skip", "true");
        assertProjectPropertiesEqual(moduleE);
    }

    @Test
    public void twoSelected_bothChanged_disableSelectedProjectsHandling() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, true);
        MavenProject moduleC = addModuleMock(AID_MODULE_C, true);
        setProjectSelections(moduleB, moduleC);
        overrideProjects(moduleB, moduleC);

        addGibProperty(Property.disableSelectedProjectsHandling, "true");

        underTest.act(config());

        verify(mavenSessionMock).setProjects(Arrays.asList(moduleB, moduleC));

        assertProjectPropertiesEqual(moduleB);
        assertProjectPropertiesEqual(moduleC);
    }

    // See "-pl :...,:..." and don't forget to call overrideProjects() if any of the moduleMocks shall _not_ be in the projects list!
    private void setProjectSelections(MavenProject... projectsToSelect) {
        List<String> selection = Arrays.stream(projectsToSelect).map(p -> ":" + p.getArtifactId()).collect(Collectors.toList());
        when(mavenExecutionRequestMock.getSelectedProjects()).thenReturn(selection);
    }

    // See "-pl !:...,!:..."
    private void setProjectDeSelections(MavenProject... projectsToDeSelect) {
        List<String> selection = Arrays.stream(projectsToDeSelect).map(p -> "!:" + p.getArtifactId()).collect(Collectors.toList());
        when(mavenExecutionRequestMock.getSelectedProjects()).thenReturn(selection);
    }
}
