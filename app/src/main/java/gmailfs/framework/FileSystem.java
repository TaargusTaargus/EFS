package gmailfs.framework;

import android.content.Context;

import java.util.HashMap;
import java.util.LinkedList;
import gmailfs.framework.Filter.FilterKey;

public class FileSystem {

    private Node root, current;
    private Path path = new Path();
    private FileDB backend;

    public FileSystem( Context context ) {
        this.backend = new FileDB( context );
        this.current = this.root = new Node( new Filter( "", "", null, null ), null );
        loadChildren();
    }

    private void loadChildren() {
        for( Filter filter : backend.retrieveChildren( path ) )
            current.addChild( filter );
    }

    public boolean isRoot() { return root.equals( current ); }

    public void addChild( Filter child ) {
        child = new Filter( getCurrentFilter().getFilterText() + " " + child.getFilterText(),
                             child.getFileTitle(), child.getFilterKey(), child.getFilterType() );
        backend.insertFilter( child, path );
        current.addChild( child );
    }

    public void back() {
        path.remove( current );
        current = current.getParent();
    }

    public void close() { backend.close(); }

    public LinkedList< Filter > currentFilters() {
        LinkedList< Filter > filters = new LinkedList< Filter >();
        for( Node child : current.children.values() )
            filters.add( child.getData() );
        return filters;
    }

    public void forward( String fileID ) {
        current = current.getChild( fileID );
        path.add( current );
        loadChildren();
    }

    //public void removeChild( String childID ) {
    //    current.removeChild( childID );
    //}

    public void root() {
        path.clear();
        current = root;
    }

    public Filter getCurrentFilter() { return current.getData(); }

    public class Node {

        private Filter data;
        private HashMap< String, Node > children = new HashMap();
        private Node parent;

        public Node( Filter data, Node parent ) {
            this.data = data;
            this.parent = parent;
        }

        public void addChild( Filter child ) {
            children.put( child.getFileTitle(), new Node( child, this ) );
        }

        public Node getChild( String fileID ) {
            return children.get( fileID );
        }

        //public void removeChild( String fileID ) {
        //    children.remove( fileID );
        //}

        public Filter getData() { return data; }
        public Node getParent() { return parent; }

    }

}