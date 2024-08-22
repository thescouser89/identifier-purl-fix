package org.example;

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import com.github.packageurl.PackageURLBuilder;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.StringUtils;
import org.commonjava.atlas.maven.ident.ref.ArtifactRef;
import org.commonjava.atlas.maven.ident.ref.SimpleArtifactRef;
import org.commonjava.atlas.maven.ident.util.ArtifactPathInfo;

import java.io.FileReader;
import java.io.Reader;

public class Main {

    public final static String MAVEN_SUBSTITUTE_EXTENSION = ".empty";


    public static void main(String[] args) throws Exception {

            Reader in = new FileReader("test.csv");

            Iterable<CSVRecord> records = CSVFormat.POSTGRESQL_CSV.parse(in);

            // Read the csv line by line and process baby!
            for (CSVRecord record : records) {

                String[] line = new String[4];
                line[0] = record.get(0);
                line[1] = record.get(1);
                line[2] = record.get(2);
                line[3] = record.get(3);

                processCsvEntry(line);
            }
    }

    public static void processCsvEntry(String[] line) {
        boolean changed = false;

        String id = line[0];
        String path = line[1];
        String existingIdentifier = line[2];
        String existingPurl = line[3];

        String newIdentifier = computeIdentifier(path);

        if (newIdentifier == null && existingIdentifier != null) {
            newIdentifier = existingIdentifier;
        }

        if (existingIdentifier == null) {
            changed = true;
        } else if (!existingIdentifier.equals(newIdentifier)) {
            changed = true;
        }

        String newPurl = computePurl(path);
        if (existingPurl == null || newPurl == null) {
            changed = true;
        } else if (!existingPurl.equals(newPurl)) {
            changed = true;
        }

        if (changed) {
            System.out.format("Path: %s\nOld: %s\nNew: %s\n----\n", path, existingIdentifier, newIdentifier);
        }
    }

    /**
     * We only need to process the MVN case for the identifier. NPM codebase for pathinfo hasn't changed and we don't
     * need to do anything for generic
     *
     * @param path
     * @return
     */
    public static String computeIdentifier(String path) {

        ArtifactPathInfo pathInfo = ArtifactPathInfo.parse(path);

        if (pathInfo == null) {
            // NCL-7238: handle cases where url has no file extension. we add the extension
            // MAVEN_SUBSTITUTE_EXTENSION and see if that helps to parse the pathInfo. Otherwise this causes
            // nasty artifact duplicates
            pathInfo = ArtifactPathInfo.parse(path + MAVEN_SUBSTITUTE_EXTENSION);
        }
        if (pathInfo != null) {
            ArtifactRef aref = new SimpleArtifactRef(
                    pathInfo.getProjectId(),
                    pathInfo.getType(),
                    pathInfo.getClassifier());
            return aref.toString();
        }
        // if we're here, that's not good
        return null;
    }

    public static String computePurl(String path) {
        ArtifactPathInfo pathInfo = ArtifactPathInfo.parse(path);
        if (pathInfo == null) {
            pathInfo = ArtifactPathInfo.parse(path + MAVEN_SUBSTITUTE_EXTENSION);
        }

        try {
            if (pathInfo != null) {
                // See https://github.com/package-url/purl-spec/blob/master/PURL-TYPES.rst#maven
                PackageURLBuilder purlBuilder = PackageURLBuilder.aPackageURL()
                        .withType(PackageURL.StandardTypes.MAVEN)
                        .withNamespace(pathInfo.getProjectId().getGroupId())
                        .withName(pathInfo.getProjectId().getArtifactId())
                        .withVersion(pathInfo.getVersion())
                        .withQualifier(
                                "type",
                                StringUtils.isEmpty(pathInfo.getType()) ? "jar" : pathInfo.getType());

                if (!StringUtils.isEmpty(pathInfo.getClassifier())) {
                    purlBuilder.withQualifier("classifier", pathInfo.getClassifier());
                }
                return purlBuilder.build().toString();
            }
        } catch (MalformedPackageURLException e) {
            System.err.println("something weird happened: " + e.toString());
            return null;
        }
        // if we're here, something very bad happened
        return null;
    }
}
