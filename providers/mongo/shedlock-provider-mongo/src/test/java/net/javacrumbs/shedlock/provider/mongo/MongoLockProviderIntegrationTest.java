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
package net.javacrumbs.shedlock.provider.mongo;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.tests.MongodForTestsFactory;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.test.support.AbstractExtensibleLockProviderIntegrationTest;
import org.bson.Document;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Date;

import static com.mongodb.client.model.Filters.eq;
import static net.javacrumbs.shedlock.provider.mongo.MongoLockProvider.ID;
import static net.javacrumbs.shedlock.provider.mongo.MongoLockProvider.LOCKED_AT;
import static net.javacrumbs.shedlock.provider.mongo.MongoLockProvider.LOCKED_BY;
import static net.javacrumbs.shedlock.provider.mongo.MongoLockProvider.LOCK_UNTIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

public class MongoLockProviderIntegrationTest extends AbstractExtensibleLockProviderIntegrationTest {
    private static MongodForTestsFactory mongoFactory;

    private static final String COLLECTION_NAME = "Shedlock";
    private static final String DB_NAME = "db";
    private MongoClient mongo;

    @Before
    public void createLockProvider() throws UnknownHostException {
        mongo = mongoFactory.newMongo();
        mongo.getDatabase(DB_NAME).drop();
    }

    @Override
    protected LockProvider getLockProvider() {
        return new MongoLockProvider(mongo, DB_NAME, COLLECTION_NAME);
    }

    @Override
    protected void assertUnlocked(String lockName) {
        Document lockDocument = getLockDocument(lockName);
        assertThat((Date) lockDocument.get(LOCK_UNTIL)).isBeforeOrEqualsTo(now());
        assertThat((Date) lockDocument.get(LOCKED_AT)).isBeforeOrEqualsTo(now());
        assertThat((String) lockDocument.get(LOCKED_BY)).isNotEmpty();
    }

    private Date now() {
        return new Date();
    }

    @Override
    protected void assertLocked(String lockName) {
        Document lockDocument = getLockDocument(lockName);
        assertThat((Date) lockDocument.get(LOCK_UNTIL)).isAfter(now());
        assertThat((Date) lockDocument.get(LOCKED_AT)).isBeforeOrEqualsTo(now());
        assertThat((String) lockDocument.get(LOCKED_BY)).isNotEmpty();
    }

    private MongoCollection<Document> getLockCollection() {
        return mongo.getDatabase(DB_NAME).getCollection(COLLECTION_NAME);
    }

    private Document getLockDocument(String lockName) {
        return getLockCollection().find(eq(ID, lockName)).first();
    }

    @BeforeClass
    public static void startMongo() throws IOException {
        mongoFactory = new MongodForTestsFactory(Version.Main.V3_2);
    }

    @AfterClass
    public static void stopMongo() {
        mongoFactory.shutdown();
    }

    @Test
    public void shouldLockWhenDocumentRemovedExternally() {
        LockProvider provider = getLockProvider();
        assertThat(provider.lock(lockConfig(LOCK_NAME1))).isNotEmpty();
        assertLocked(LOCK_NAME1);

        DeleteResult result = getLockCollection().deleteOne(eq(ID, LOCK_NAME1));
        assumeThat(result.getDeletedCount()).isEqualTo(1);

        assertThat(provider.lock(lockConfig(LOCK_NAME1))).isNotEmpty();
        assertLocked(LOCK_NAME1);
    }
}