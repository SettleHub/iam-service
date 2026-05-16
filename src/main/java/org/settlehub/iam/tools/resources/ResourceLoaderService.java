package org.settlehub.iam.tools.resources;

import java.io.IOException;
import java.io.InputStream;
import lombok.Setter;
import org.settlehub.iam.tools.interfaces.IResourceLoaderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component("resourceLoaderService")
public class ResourceLoaderService implements IResourceLoaderService, ResourceLoaderAware {
    @Setter
    private ResourceLoader resourceLoader;
    private final Logger logger = LoggerFactory.getLogger(ResourceLoaderService.class);

    public InputStream getInputStreamFromResourceFile(Resource resource) throws IOException {
        return resource.getInputStream();
    }

    public InputStream getInputStreamFromResourceFile(String location) throws IOException {
        Resource resource = resourceLoader.getResource(location);
        return resource.getInputStream();
    }
}
