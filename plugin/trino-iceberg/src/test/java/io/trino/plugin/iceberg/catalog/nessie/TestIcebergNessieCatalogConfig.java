/*
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
package io.trino.plugin.iceberg.catalog.nessie;

import com.google.common.collect.ImmutableMap;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.Map;

import static io.airlift.configuration.testing.ConfigAssertions.assertFullMapping;
import static io.airlift.configuration.testing.ConfigAssertions.assertRecordedDefaults;
import static io.airlift.configuration.testing.ConfigAssertions.recordDefaults;

public class TestIcebergNessieCatalogConfig
{
    @Test
    public void testDefaults()
    {
        assertRecordedDefaults(recordDefaults(IcebergNessieCatalogConfig.class)
                .setDefaultWarehouseDir(null)
                .setServerUri(null)
                .setAuthenticationType(null)
                .setBearerToken(null)
                .setReadTimeoutMillis(null)
                .setConnectTimeoutMillis(null)
                .setCompressionEnabled(true)
                .setDefaultReferenceName("main"));
    }

    @Test
    public void testExplicitPropertyMapping()
    {
        Map<String, String> properties = ImmutableMap.<String, String>builder()
                .put("iceberg.nessie-catalog.default-warehouse-dir", "/tmp")
                .put("iceberg.nessie-catalog.uri", "http://localhost:xxx/api/v1")
                .put("iceberg.nessie-catalog.ref", "someRef")
                .put("iceberg.nessie-catalog.auth.type", "NONE")
                .put("iceberg.nessie-catalog.auth.bearer.token", "bearerToken")
                .put("iceberg.nessie-catalog.compression-enabled", "false")
                .put("iceberg.nessie-catalog.connect-timeout-ms", "123")
                .put("iceberg.nessie-catalog.read-timeout-ms", "456")
                .buildOrThrow();

        IcebergNessieCatalogConfig expected = new IcebergNessieCatalogConfig()
                .setDefaultWarehouseDir("/tmp")
                .setServerUri(URI.create("http://localhost:xxx/api/v1"))
                .setDefaultReferenceName("someRef")
                .setAuthenticationType(AuthenticationType.NONE)
                .setBearerToken("bearerToken")
                .setCompressionEnabled(false)
                .setConnectTimeoutMillis(123)
                .setReadTimeoutMillis(456);

        assertFullMapping(properties, expected);
    }
}
