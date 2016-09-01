package gmailfs.framework;

import android.content.Context;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class FileSystem {

    private Node root, current;
    private Path path = new Path();//AppContext.path;
    private FileDB backend;

    public FileSystem( Context context ) {
        this.backend = new FileDB( context );
        backend.printDBtoLogcat();
        this.current = this.root = new Node( new Filter( "", "", null ) );
        path.add( current );
        loadChildren();
    }

    private void loadChildren() {
        for( Filter filter : backend.retrieveFiltersFromPath( path ) )
            current.addChild( filter );
    }

    public boolean isRoot() { return root.equals( current ); }

    public LinkedList< Filter > currentFilters() {
        LinkedList< Filter > filters = new LinkedList();
        for( Node child : current.children.values() )
            filters.add( child.getData() );
        return filters;
    }

    public LinkedList< File > currentFiles() {
        return backend.retrieveFilesFromQuery( path.getFilterText(), current.getData().getFilterKey() );
    }

    public void addFilter( Filter child ) {
        backend.insertFilter( child, path );
        current.addChild( child );
    }

    public void addFiles( List< File > files ) {
        backend.insertFiles( files, path );
    }

    public void back() {
        path.remove( current );
        current = path.getLast();
    }

    public void close() { backend.close(); }

    public void editFilter( Filter old, Filter newFilter ) {
        backend.updateFilter( old, newFilter, path );
        current.removeChild( old.getFilterID() );
        current.addChild( newFilter );
    }

    public void forward( String fileID ) {
        current = current.getChild( fileID );
        path.add( current );
        loadChildren();
    }

    public void removeFilter( Filter filter ) {
        backend.recursiveRemoveFilter( filter, path );
        current.removeChild( filter.getFilterID() );
    }

    public void root() {
        path.clear();
        current = root;
    }

    public Filter getCurrentFilter() { return current.getData(); }
    public int totalFilesInDB() { return backend.getRowCount( FileDB.ITEM_TABLE_NAME ); }
    public Node getCurrentNode() { return current; }
    public Path getPath() { return path; }

    public class Node {

        private Filter data;
        private HashMap< String, Node > children = new HashMap();

        public Node( Filter data ) {
            this.data = data;
        }

        public void addChild( Filter child ) {
            children.put( child.getFilterID(), new Node( child ) );
        }

        public void removeChild( String id ) {
            children.remove( id );
        }

        public boolean hasChildren() { return !children.isEmpty(); }
        public Filter getData() { return data; }
        public Node getChild( String fileID ) {
            return children.get( fileID );
        }

    }

}