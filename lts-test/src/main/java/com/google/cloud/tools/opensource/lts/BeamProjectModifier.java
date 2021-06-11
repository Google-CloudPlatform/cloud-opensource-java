/*
 * Copyright 2021 Google LLC.
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

package com.google.cloud.tools.opensource.lts;

import com.google.cloud.tools.opensource.dependencies.Bom;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import com.google.common.io.MoreFiles;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Modifies Apache Beam's build files to use the libraries in the BOM when running the unit tests.
 * Due to its complex build configuration Apache Beam requires special handling.
 */
class BeamProjectModifier implements BuildFileModifier {
  @Override
  public void modifyFiles(String name, Path projectDirectory, Bom bom)
      throws TestSetupFailureException {
    Iterable<Path> paths = MoreFiles.fileTraverser().breadthFirst(projectDirectory);
    try {
      for (Path path : paths) {
        if (path.endsWith("build.gradle")) {
          modifyBeamGradleFile(path, bom);
        } else if (path.endsWith("BeamModulePlugin.groovy")) {
          modifyBeamModulePlugin(path);
        }
      }
    } catch (IOException ex) {
      throw new TestSetupFailureException("Unable to modify the build file", ex);
    }
  }

  // build.gradle files that run as part of the test in the beam section of repositories.yaml
  private static final ImmutableList<Path> beamTestSubprojects =
      ImmutableList.of(
          Paths.get("sdks", "java", "core", "build.gradle"),
          Paths.get("sdks", "java", "io", "google-cloud-platform", "build.gradle"),
          Paths.get("sdks", "java", "extensions", "google-cloud-platform-core", "build.gradle"),
          Paths.get("runners", "google-cloud-dataflow-java", "build.gradle"));

  private static void modifyBeamGradleFile(Path gradleFile, Bom bom) throws IOException {

    // Beam already uses enforcedPlatform(google_cloud_platform_libraries_bom), which prevents
    // gcp-lts-bom's setting gRPC library version.
    if (beamTestSubprojects.stream().anyMatch(gradleFile::endsWith)) {
      String buildGradleContent =
          Files.asCharSource(gradleFile.toFile(), StandardCharsets.UTF_8).read();

      String bomCoordinates = bom.getCoordinates();
      buildGradleContent =
          buildGradleContent.replaceAll(
              "\ndependencies \\{",
              "\ndependencies {\n    compile enforcedPlatform('"
                  + bomCoordinates
                  + "')\n"
                  + "    testRuntime enforcedPlatform('"
                  + bomCoordinates
                  + "')\n"
                  + "    testRuntime library.java.junit\n"
                  + "    testRuntime library.java.hamcrest_core\n"
                  // This shouldn't be needed. But without this, GcsUtilTest fails
                  // with NoSuchMethodError on CacheBuilder.expireAfterWrite(Ljava/time/Duration;)
                  + "    testRuntime \"com.google.guava:guava:30.1-jre\"\n"
                  + "    testRuntime library.java.hamcrest_library\n");

      // Tried compileOnly but analyzeTestClassesDependencies's configuratin cannot resolve
      // the dependencies.
      // https://github.com/GoogleCloudPlatform/cloud-opensource-java/pull/1982#discussion_r610878573
      // buildGradleContent =
      //    buildGradleContent.replaceAll(
      //      "compile enforcedPlatform\\(library.java.google_cloud_platform_libraries_bom\\)",
      //      "compileOnly enforcedPlatform(library.java.google_cloud_platform_libraries_bom)");
      Files.asCharSink(gradleFile.toFile(), StandardCharsets.UTF_8).write(buildGradleContent);
    }
  }

  private static void modifyBeamModulePlugin(Path beamModulePluginFile) throws IOException {
    // We don't want Beam to `force` dependencies when we run tests, because the `force` overrides
    // library versions set in the gcp-lts-bom.
    // https://github.com/GoogleCloudPlatform/cloud-opensource-java/pull/1982#discussion_r611911325
    String buildGradleContent =
        Files.asCharSource(beamModulePluginFile.toFile(), StandardCharsets.UTF_8).read();

    String buildGradleContentTestRuntimeClassPathModified =
        buildGradleContent.replaceAll(
            "config.getName\\(\\) != \"errorprone\"",
            "![\"errorprone\", \"testRuntimeClasspath\"].contains(config.getName())");

    Verify.verify(
        !buildGradleContentTestRuntimeClassPathModified.equals(buildGradleContent),
        "The step should modify BeamModulePlugin.groovy");

    Files.asCharSink(beamModulePluginFile.toFile(), StandardCharsets.UTF_8)
        .write(buildGradleContentTestRuntimeClassPathModified);
  }
}
