package gmailfs.framework;

import java.util.LinkedList;
import gmailfs.framework.FileSystem.Node;

public class Path extends LinkedList< Node > {

    public static final String FILE_DELIMITER = "/";

    public String toString() {
        String path = "/";
        for( Node el : this )
            path += el.getData().getFileTitle() + Path.FILE_DELIMITER;
        return path;
    }

}