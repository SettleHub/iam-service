package org.settlehub.iam.tools.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Metadata {
    private String filename;

    private String fileExtension;

    private String mimeType;

    private String createdAt;
}