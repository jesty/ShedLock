/**
 * Copyright 2009-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.javacrumbs.shedlock.core;

import org.junit.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class LockConfigurationTest {

    @Test
    public void lockAtLeastUnitilShouldBeBeforeOrEqualsToLockAtMostUntil() {
        Instant time = Instant.now().plusSeconds(5);
        new LockConfiguration("name", time, time);
        new LockConfiguration("name", time.plusMillis(1), time);

        assertThatThrownBy(() -> new LockConfiguration("name", time, time.plusMillis(1))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void lockAtMostUntilHasToBeInTheFuture() {
        Instant now = Instant.now();
        assertThatThrownBy(() -> new LockConfiguration("name", now.minusSeconds(1))).isInstanceOf(IllegalArgumentException.class);
    }


    @Test
    public void nameShouldNotBeEmpty() {
        assertThatThrownBy(() -> new LockConfiguration("", Instant.now().plusSeconds(5))).isInstanceOf(IllegalArgumentException.class);
    }

}