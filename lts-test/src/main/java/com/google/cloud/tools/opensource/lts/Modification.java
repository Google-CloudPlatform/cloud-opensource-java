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

import com.google.common.base.VerifyException;

/** The type of how to modify build files of a project. */
enum Modification {
  MAVEN,
  GRADLE,
  BEAM;

  BuildFileModifier getModifier() {
    if (this == MAVEN) {
      return new MavenProjectModifier();
    } else if (this == GRADLE) {
      return new GradleProjectModifier();
    } else if (this == BEAM) {
      return new BeamProjectModifier();
    } else {
      throw new VerifyException("Unexpected modification: " + this);
    }
  }
}
