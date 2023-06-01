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

import io.airlift.configuration.Config;
import io.airlift.configuration.ConfigDescription;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import java.net.URI;
import java.util.Optional;

public class IcebergNessieCatalogConfig
{
    private String defaultReferenceName = "main";
    private String defaultWarehouseDir;
    private URI serverUri;
    private Optional<AuthenticationType> authenticationType = Optional.empty();
    private Optional<String> bearerToken = Optional.empty();
    private Optional<Integer> readTimeoutMillis = Optional.empty();
    private Optional<Integer> connectTimeoutMillis = Optional.empty();
    private boolean compressionEnabled = true;

    @NotNull
    public String getDefaultReferenceName()
    {
        return defaultReferenceName;
    }

    @Config("iceberg.nessie-catalog.ref")
    @ConfigDescription("The default Nessie reference to work on")
    public IcebergNessieCatalogConfig setDefaultReferenceName(String defaultReferenceName)
    {
        this.defaultReferenceName = defaultReferenceName;
        return this;
    }

    @NotNull
    public URI getServerUri()
    {
        return serverUri;
    }

    @Config("iceberg.nessie-catalog.uri")
    @ConfigDescription("The URI to connect to the Nessie server")
    public IcebergNessieCatalogConfig setServerUri(URI serverUri)
    {
        this.serverUri = serverUri;
        return this;
    }

    @NotEmpty
    public String getDefaultWarehouseDir()
    {
        return defaultWarehouseDir;
    }

    @Config("iceberg.nessie-catalog.default-warehouse-dir")
    @ConfigDescription("The default warehouse to use for Nessie")
    public IcebergNessieCatalogConfig setDefaultWarehouseDir(String defaultWarehouseDir)
    {
        this.defaultWarehouseDir = defaultWarehouseDir;
        return this;
    }

    @Config("iceberg.nessie-catalog.auth.type")
    @ConfigDescription("The authentication type to use. Available values are NONE | BEARER. Default: NONE")
    public IcebergNessieCatalogConfig setAuthenticationType(AuthenticationType authenticationType)
    {
        this.authenticationType = Optional.ofNullable(authenticationType);
        return this;
    }

    public Optional<AuthenticationType> getAuthenticationType()
    {
        return authenticationType;
    }

    @Config("iceberg.nessie-catalog.auth.bearer.token")
    @ConfigDescription("The token to use with BEARER authentication")
    public IcebergNessieCatalogConfig setBearerToken(String token)
    {
        this.bearerToken = Optional.ofNullable(token);
        return this;
    }

    public Optional<String> getBearerToken()
    {
        return bearerToken;
    }

    @Config("iceberg.nessie-catalog.read-timeout-ms")
    @ConfigDescription("The read timeout in milliseconds for the client. Default: 25000")
    public IcebergNessieCatalogConfig setReadTimeoutMillis(Integer readTimeoutMillis)
    {
        this.readTimeoutMillis = Optional.ofNullable(readTimeoutMillis);
        return this;
    }

    public Optional<Integer> getReadTimeoutMillis()
    {
        return readTimeoutMillis;
    }

    @Config("iceberg.nessie-catalog.connect-timeout-ms")
    @ConfigDescription("The connection timeout in milliseconds for the client. Default: 5000")
    public IcebergNessieCatalogConfig setConnectTimeoutMillis(Integer connectTimeoutMillis)
    {
        this.connectTimeoutMillis = Optional.ofNullable(connectTimeoutMillis);
        return this;
    }

    public Optional<Integer> getConnectTimeoutMillis()
    {
        return connectTimeoutMillis;
    }

    @Config("iceberg.nessie-catalog.compression-enabled")
    @ConfigDescription("Configure whether compression should be enabled or not. Default: true")
    public IcebergNessieCatalogConfig setCompressionEnabled(boolean compressionEnabled)
    {
        this.compressionEnabled = compressionEnabled;
        return this;
    }

    public boolean isCompressionEnabled()
    {
        return compressionEnabled;
    }
}
