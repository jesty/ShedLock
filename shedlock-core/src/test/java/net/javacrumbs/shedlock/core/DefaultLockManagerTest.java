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
import org.mockito.InOrder;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class DefaultLockManagerTest {

    public static final LockConfiguration LOCK_CONFIGURATION = new LockConfiguration("name", Instant.now().plusSeconds(10));
    private final LockProvider lockProvider = mock(LockProvider.class);
    private final LockConfigurationExtractor lockConfigurationExtractor = mock(LockConfigurationExtractor.class);
    private final Runnable task = mock(Runnable.class);
    private final SimpleLock lock = mock(SimpleLock.class);

    private final DefaultLockManager defaultLockManager = new DefaultLockManager(lockProvider, lockConfigurationExtractor);


    @Test
    public void noConfigNoLock() {
        when(lockConfigurationExtractor.getLockConfiguration(task)).thenReturn(Optional.empty());

        defaultLockManager.executeWithLock(task);
        verify(task).run();
        verifyZeroInteractions(lockProvider);
    }

    @Test
    public void executeIfLockAvailable() {
        when(lockConfigurationExtractor.getLockConfiguration(task)).thenReturn(Optional.of(LOCK_CONFIGURATION));
        when(lockProvider.lock(LOCK_CONFIGURATION)).thenReturn(Optional.of(lock));

        defaultLockManager.executeWithLock(task);
        verify(task).run();
        InOrder inOrder = inOrder(task, lock);
        inOrder.verify(task).run();
        inOrder.verify(lock).unlock();
    }

    @Test
    public void doNotExecuteIfAlreadyLocked() {
        when(lockConfigurationExtractor.getLockConfiguration(task)).thenReturn(Optional.of(LOCK_CONFIGURATION));
        when(lockProvider.lock(LOCK_CONFIGURATION)).thenReturn(Optional.empty());

        defaultLockManager.executeWithLock(task);
        verifyZeroInteractions(task);
    }

}