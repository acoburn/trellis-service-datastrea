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
package org.trellisldp.binary;

import static java.nio.file.Files.copy;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.IRI;
import org.slf4j.Logger;
import org.trellisldp.api.Binary;
import org.trellisldp.spi.BinaryService;

/**
 * @author acoburn
 */
public class FileResolver implements BinaryService.Resolver {

    private static final String UNSUPPORTED_MESSAGE = "File Resolver does not support multipart uploads";

    private static final Logger LOGGER = getLogger(FileResolver.class);

    private final Map<String, String> partitions;

    /**
     * Create a File-based Binary Resolver
     * @param partitions a mapping of partition locations
     */
    public FileResolver(final Map<String, String> partitions) {
        this.partitions = unmodifiableMap(partitions);
    }

    @Override
    public List<String> getUriSchemes() {
        return singletonList("file");
    }

    @Override
    public Optional<InputStream> getContent(final String partition, final IRI identifier) {
        return getFileFromIdentifier(partition, identifier).map(file -> {
            try {
                return new FileInputStream(file);
            } catch (final FileNotFoundException ex) {
                throw new UncheckedIOException(ex);
            }
        });
    }

    @Override
    public Boolean exists(final String partition, final IRI identifier) {
        return getFileFromIdentifier(partition, identifier).filter(File::isFile).isPresent();
    }

    @Override
    public Boolean supportsMultipartUpload() {
        return false;
    }

    @Override
    public String initiateUpload(final String partition, final IRI identifier, final String mimeType) {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public String uploadPart(final String identifier, final Integer partNumber, final InputStream content) {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public Binary completeUpload(final String identifier, final Map<Integer, String> partDigests) {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public void abortUpload(final String identifier) {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public Stream<Map.Entry<Integer, String>> listParts(final String identifier) {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    // TODO -- support incoming digest comparisons
    @Override
    public void setContent(final String partition, final IRI identifier, final InputStream stream,
            final Map<String, String> metadata) {
        requireNonNull(stream, "InputStream may not be null!");
        getFileFromIdentifier(partition, identifier).map(File::toPath).ifPresent(path -> {
            LOGGER.debug("Setting binary content for {}", identifier.getIRIString());
            try {
                copy(stream, path, REPLACE_EXISTING);
            } catch (final IOException ex) {
                LOGGER.error("Error while setting content: {}", ex.getMessage());
                throw new UncheckedIOException(ex);
            }
        });
    }

    private Optional<File> getFileFromIdentifier(final String partition, final IRI identifier) {
        return ofNullable(identifier).map(IRI::getIRIString).map(URI::create).map(URI::getSchemeSpecificPart)
            .filter(x -> partitions.containsKey(partition)).map(x -> new File(partitions.get(partition), x));
    }
}
