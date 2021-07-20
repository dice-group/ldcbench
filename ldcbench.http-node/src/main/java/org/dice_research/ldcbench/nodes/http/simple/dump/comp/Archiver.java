package org.dice_research.ldcbench.nodes.http.simple.dump.comp;

import java.io.File;

import org.apache.commons.compress.archivers.ArchiveOutputStream;

/**
 * The interface for an Archiver that create archive Stream, build archive and store
 * additional informations such as mediaType and fileExtension
 *
 * @author Ulrich
 *
 */
public interface Archiver {

    /**
     * Create an Archive of the dumpFile using the outputstream of the Archiver
     *
     * @param dumpFile
     *          the file to put in the Archive
     * @param archivename
     *          the name of the created archive
     * @throws IOException
     */
    public void buildArchive(File archive, File dumpFile);

    /**
     * add a file to the archive
     *
     * @param dumpFile
     *          the file to add
     */
    public void addFileToArchive(ArchiveOutputStream aos, File dumpFile);

    /**
     * initialize the outputStream and set everything up for the creation of the archive
     *
     * @param archive
     *          the name of the archive that will be created
     *
     */
     public ArchiveOutputStream createStream(File archive);

     /**
      * Returns the media type of the used archiver. In the ideal case, this type
      * is following the <a href=
      * "https://www.iana.org/assignments/media-types/media-types.xhtml">definitions
      * of the IANA</a>.
      *
      * @return the media type of the used compression.
      */
     public String getMediaType();

     /**
      * Returns the typical file name extension for the used archiver (including
      * the '.' character if there is any).
      *
      * @return the typical file name extension for the used compression.
      */
     public String getFileNameExtension();
}