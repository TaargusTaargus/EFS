package gmailfs.framework;

import java.util.LinkedList;
import gmailfs.framework.FileSystem.Node;

public class Path extends LinkedList< Node > {

    public static final String FILE_DELIMITER = "/";

    private LinkedList< String > filterPath = new LinkedList< String >();

    public boolean add( Node node ) {
        filterPath.add( node.getData().getFilterText() );
        return super.add( node );
    }

    public boolean remove( Node node ) {
        filterPath.remove( node.getData().getFilterText() );
        return super.remove( node );
    }

    public String getFilterText() {
        String filter = "";
        if( !filterPath.isEmpty() )
            filter += filterPath.getFirst();
        for( int i = 1; i < filterPath.size() ; i++ )
            filter += " " + filterPath.get( i );
        return filter;
    }

    public String getFilterPath() {
        String path = "";
        for( Node el : this )
            path += el.getData().getFilterID() + Path.FILE_DELIMITER;
        return path;
    }

    public void clear() {
        super.clear();
        filterPath.clear();
    }

}