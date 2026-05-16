package org.settlehub.iam.tools.interfaces;

import org.springframework.core.io.Resource;
import java.io.IOException;
import java.io.InputStream;

public interface IResourceLoaderService {

    InputStream getInputStreamFromResourceFile(Resource resource) throws IOException;

    InputStream getInputStreamFromResourceFile(String location) throws IOException;

}
